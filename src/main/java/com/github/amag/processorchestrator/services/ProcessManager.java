package com.github.amag.processorchestrator.services;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.context.ProcessContext;
import com.github.amag.processorchestrator.criteria.Criteria;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.*;
import com.github.amag.processorchestrator.interceptor.ProcessInstanceChangeInterceptor;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.ProcessInstanceStateMachineConfig;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessManager {

    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final ArangoOperations arangoOperations;
    private final StateMachineFactory<ProcessInstanceStatus, ProcessInstanceEvent> processInstanceStateMachineFactory;
    private final ProcessInstanceChangeInterceptor processInstanceChangeInterceptor;

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
                        ProcessInstance savedProcessInstance = processInstanceRepository.save(processInstance);
                        log.debug("Process Date {} saved process instance id {}",date, savedProcessInstance.getArangoId());
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

    // Not in use
    public void startSingleProcess(){

        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.PENDING, false);
        log.debug("Found process instance? {} ",optionalProcessInstance.isPresent());

        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            foundProcessInstance.setStatus(ProcessInstanceStatus.READY);
            processInstanceRepository.save(foundProcessInstance);
            ProcessContext processContext = new ProcessContext();
            foundProcessInstance.setStatus(ProcessInstanceStatus.INPROGRESS);
            foundProcessInstance.setProcessContext(processContext);
            processInstanceRepository.save(foundProcessInstance);
        }, () ->
                log.debug("Didn't find any pending process instance"));
    }

    public void startMultipleProcess() {

        List<ProcessInstance> processInstances = processInstanceRepository.findAllByStatusAndIsTemplate(ProcessInstanceStatus.PENDING, false);
        if (processInstances != null && processInstances.size() > 0) {
            processInstances.forEach(foundProcessInstance -> {
                foundProcessInstance.setStatus(ProcessInstanceStatus.READY);
                processInstanceRepository.save(foundProcessInstance);
            });
        }
        List<ProcessInstance> readyProcessInstances = processInstanceRepository.findAllByStatusAndIsTemplate(ProcessInstanceStatus.READY, false);
        if (readyProcessInstances != null && readyProcessInstances.size() > 0) {
            readyProcessInstances.forEach(foundProcessInstance -> {
                sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.PICKEDUP, ProcessInstanceStatus.INPROGRESS);
            });
        }
    }

    public void completeProcess(){

        List<ProcessInstance> processInstances = processInstanceRepository.findCompletedProcessInstances(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.COMPLETED);
        if (processInstances != null && processInstances.size()>0){
            log.debug("Found {} completed process instance",processInstances.size());
            processInstances.forEach(processInstance -> {
                sendProcessInstanceEvent(UUID.fromString(processInstance.getArangoKey()),ProcessInstanceEvent.FINISHED,ProcessInstanceStatus.COMPLETED);
            });
        } else {
            log.debug("Didn't find any completed process instances");
        }
    }

    private class TaskInstance2TaskInstance
    {
        public TaskInstance from, to;
    }

    public void sendProcessInstanceEvent(UUID instanceId, ProcessInstanceEvent processInstanceEvent, ProcessInstanceStatus targetStatusEnum ){
        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findById(instanceId);
        optionalProcessInstance.ifPresentOrElse(processInstance -> {
            StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine = build(processInstance);
            Message message
                    = MessageBuilder.withPayload(processInstanceEvent)
                    .setHeader(ProcessInstanceStateMachineConfig.PROCESS_INSTANCE_ID_HEADER,processInstance.getArangoKey())
                    .build();
            stateMachine.sendEvent(message);
            awaitForStatus(UUID.fromString(processInstance.getArangoKey()), targetStatusEnum);
            if(stateMachine.hasStateMachineError()){
                sendProcessInstanceEvent(UUID.fromString(processInstance.getArangoKey()), ProcessInstanceEvent.ERROR_OCCURRED, ProcessInstanceStatus.FAILED);
            }
        },() -> {
            log.error("Error while sending event");
        });
    }

    private void awaitForStatus(UUID instanceId, ProcessInstanceStatus statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            processInstanceRepository.findById(instanceId).ifPresentOrElse(processInstance -> {
                if (statusEnum.equals(processInstance.getStatus())) {
                    found.set(true);
                    log.debug("Instance Found");
                } else {
                    log.debug("Instance Status Not Equal. Expected: " + statusEnum.name() + " Found: " + processInstance.getStatus().name());
                }
            }, () -> {
                log.debug("Instance Id Not Found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }

    }


    private StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> build(ProcessInstance processInstance){
        StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine = processInstanceStateMachineFactory.getStateMachine(UUID.fromString(processInstance.getArangoKey()));
        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(
                        sma -> {
                            sma.addStateMachineInterceptor(processInstanceChangeInterceptor);
                            sma.resetStateMachine(new DefaultStateMachineContext<>(processInstance.getStatus(), null,null,null));
                        }
                );
        stateMachine.start();
        return stateMachine;
    }

}
