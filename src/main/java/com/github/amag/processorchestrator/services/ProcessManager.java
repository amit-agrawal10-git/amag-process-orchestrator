package com.github.amag.processorchestrator.services;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.criteria.Criteria;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.events.ProcessEventManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessManager {

    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final ProcessEventManager processEventManager;
    private final ArangoOperations arangoOperations;

    public void instantiateJob() {

        Optional<Process> optionalProcess = processRepository.findByExecutedUptoBeforeAndProcessStatus(new Date(), ProcessStatus.AVAILABLE);

        optionalProcess.ifPresentOrElse(process -> {
                    process.setProcessStatus(ProcessStatus.INPROGRESS);
                    processRepository.save(process);

                    Date startDate = (process.getExecutedUpto() != null) ? process.getExecutedUpto() : process.getFrom();
                    Calendar start = Calendar.getInstance();
                    start.set(Calendar.HOUR_OF_DAY, 0);
                    start.set(Calendar.MINUTE, 0);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);
                    start.setTime(startDate);
                    start.add(Calendar.DATE, 1);

                    Calendar end = Calendar.getInstance();
                    end.setTime(new Date());

                    for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1),
                            date = start.getTime()) {

                        ProcessInstance processInstance = process.getProcessTemplate()
                                .toBuilder()
                                .processDate(date)
                                .processTemplate(process.getProcessTemplate())
                                .isTemplate(false)
                                .build()
                                ;
                        Criteria<ProcessInstance> processInstanceCriteria = null;
                        if((processInstanceCriteria = processInstance.getInstantiationCriteria()) != null){
                            if (!processInstanceCriteria.evaluate(processInstance, arangoOperations).isCriteriaResult()){
                                continue;
                            }
                        }

                        List<TaskInstance> taskInstances = taskInstanceRepository.findLastTaskInstancesByProcessTemplateArangoId(process.getProcessTemplate().getArangoId());
                      //  loadAllTaskInstanceTemplate(taskInstances);
                        List<TaskInstance> newTaskInstances = new ArrayList<TaskInstance>();
                        List<TaskInstance2TaskInstance> tempInstances = new ArrayList<>();
                        copy(processInstance, null, taskInstances, newTaskInstances, tempInstances);
                        tempInstances.forEach(x -> {
                            x.from.getDependsOn().add(x.to);
                        });
                        arangoOperations.repsert(processInstance);
                        for (int i = newTaskInstances.size() - 1; i >= 0; i--)
                            arangoOperations.repsert(newTaskInstances.get(i));
                        process.setExecutedUpto(date);
                        processRepository.save(process);
                    }
                    process.setProcessStatus(ProcessStatus.AVAILABLE);
                    processRepository.save(process);
                }
        , () -> {
                    log.debug("Didn't find any pending process");
                });
    }

    private void loadAllTaskInstanceTemplate(List<TaskInstance> taskInstances){
        if (taskInstances != null) {
            taskInstances.stream().forEach(x ->
            {
                loadAllTaskInstanceTemplate(x.getDependsOn());
            });
        }
    }

    private void copy(ProcessInstance processInstance,TaskInstance dependentInstance, List<TaskInstance> taskInstances, List<TaskInstance> resultTaskInstances, List<TaskInstance2TaskInstance> instance2TaskInstances) {
        if (taskInstances != null) {
            taskInstances.stream().forEach(x ->
            {
                x.setTaskTemplate(arangoOperations.find(UUID.fromString(x.getArangoKey()),TaskInstance.class).get());
                x.setArangoId(null);
                x.setArangoKey(null);
                x.setTemplate(false);
                x.setCreatedWhen(null);
                x.setModifiedWhen(null);
                x.setBaseAction(null);
                x.setDescription(null);
                x.setProcessTemplate(null);
                x.setProcessInstance(processInstance);
                if (resultTaskInstances.contains(x)) {
                    TaskInstance y = resultTaskInstances.get(resultTaskInstances.indexOf(x));
                    if (dependentInstance != null)
                        insertToTempList(dependentInstance, y, instance2TaskInstances);
                } else {
                    resultTaskInstances.add(x);
                }
            });
            taskInstances.stream().forEach(x ->
            {
                if (x.getDependsOn() != null)
                    copy(processInstance, x, x.getDependsOn(), resultTaskInstances, instance2TaskInstances);
            });
        }
    }

    private void insertToTempList(TaskInstance from, TaskInstance to, List<TaskInstance2TaskInstance> instance2TaskInstances){
        TaskInstance2TaskInstance taskInstance2TaskInstance = new TaskInstance2TaskInstance();
        taskInstance2TaskInstance.from=from;
        taskInstance2TaskInstance.to=to;
        instance2TaskInstances.add(taskInstance2TaskInstance);
    }

    public void findAndMarkReady() {
        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.PENDING,ProcessInstanceEvent.DEPENDENCY_RESOLVED);
        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            Criteria<ProcessInstance> processInstanceCriteria = foundProcessInstance.getExecutionCriteria();
            boolean output = true;
            if(processInstanceCriteria!=null)
                output = processInstanceCriteria.evaluate(foundProcessInstance, arangoOperations).isCriteriaResult();
            if (output)
                processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.DEPENDENCY_RESOLVED);
            else
                processEventManager.rollbackEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.DEPENDENCY_RESOLVED,arangoOperations);
        }, ()-> log.debug("No pending process instance found"));
    }

    public void startProcess(final int maximumActiveProcess) {
        long activeCount = processInstanceRepository.countByStatus(ProcessInstanceStatus.INPROGRESS);
        if (activeCount < maximumActiveProcess) {
            Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.READY,ProcessInstanceEvent.PICKEDUP);
            optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
                processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.PICKEDUP);
            }, ()-> log.debug("No ready process instance found"));
        } else {
            log.debug("Maximum number of process are already running");
        }
    }

    public void completeProcess(){
        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findCompletedProcessInstance(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.INPROGRESS,ProcessInstanceEvent.FINISHED);
        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.FINISHED);
            }, ()-> log.debug("No process instance found to be completed"));
    }

    private class TaskInstance2TaskInstance
    {
        public TaskInstance from, to;
    }

}
