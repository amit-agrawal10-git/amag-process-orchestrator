package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.context.ProcessContext;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessManager {

    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;

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


                        List<TaskInstance> taskInstances = taskInstanceRepository.findLastTaskInstancesByProcessTemplateArangoId(process.getProcessTemplate().getArangoId());
                        List<TaskInstance> newTaskInstances = new ArrayList<TaskInstance>();
                        List<TaskInstance2TaskInstance> tempInstances = new ArrayList<>();
                        copy(processInstance, null, taskInstances, newTaskInstances, tempInstances);
                        tempInstances.forEach(x -> {
                            x.from.getDependsOn().add(x.to);
                        });
                        processInstanceRepository.save(processInstance);
                        for (int i = newTaskInstances.size() - 1; i >= 0; i--)
                            taskInstanceRepository.save(newTaskInstances.get(i));
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

    private void copy(ProcessInstance processInstance,TaskInstance dependentInstance, List<TaskInstance> taskInstances, List<TaskInstance> resultTaskInstances, List<TaskInstance2TaskInstance> instance2TaskInstances) {
        if (taskInstances != null) {
            taskInstances.stream().forEach(x ->
            {
                x.setTaskTemplate(taskInstanceRepository.findById(UUID.fromString(x.getArangoKey())).get());
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

    public void startProcess(){

        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.PENDING, false);
        log.debug("Found process instance? {} ",optionalProcessInstance.isPresent());

        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            foundProcessInstance.setStatus(ProcessInstanceStatus.RESERVED);
            processInstanceRepository.save(foundProcessInstance);
            ProcessContext processContext = new ProcessContext();
            foundProcessInstance.setStatus(ProcessInstanceStatus.INPROGRESS);
            foundProcessInstance.setProcessContext(processContext);
            processInstanceRepository.save(foundProcessInstance);
        }, () ->
                log.debug("Didn't find any pending process instance"));
    }

    public void completeProcess(){

        List<ProcessInstance> processInstances = processInstanceRepository.findCompletedProcessInstances(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.COMPLETED);
        if (processInstances != null && processInstances.size()>0){
            log.debug("Found {} completed process instance",processInstances.size());
            processInstances.forEach(processInstance -> {
                processInstance.setStatus(ProcessInstanceStatus.COMPLETED);
                processInstanceRepository.save(processInstance);
            });
        } else {
            log.debug("Didn't find any completed process instances");
        }
    }

    private class TaskInstance2TaskInstance
    {
        public TaskInstance from, to;
    }

}
