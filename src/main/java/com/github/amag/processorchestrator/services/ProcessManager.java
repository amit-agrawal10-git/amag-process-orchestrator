package com.github.amag.processorchestrator.services;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.platform.criteria.Criteria;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.process.actions.BaseProcessAction;
import com.github.amag.processorchestrator.repositories.ProcessArangoRepository;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.events.ProcessEventManager;
import com.github.amag.processorchestrator.web.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessManager {

    private final ProcessArangoRepository processArangoRepository;
    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessEventManager processEventManager;
    private final ApplicationContext applicationContext;
    private final ArangoOperations arangoOperations;
    private final TaskInstanceRepository taskInstanceRepository;

    public static Predicate<ProcessInstance> isReady = new Predicate<ProcessInstance>() {
        @Override
        public boolean test(ProcessInstance processInstance) {
            return processInstance.getStatus().equals(ProcessInstanceStatus.READY);
        }
    };

    public static Predicate<ProcessInstance> isProcessInProgress = new Predicate<ProcessInstance>() {
        @Override
        public boolean test(ProcessInstance processInstance) {
            return processInstance.getStatus().equals(ProcessInstanceStatus.INPROGRESS);
        }
    };

    public static Predicate<TaskInstance> isTaskCompleted = new Predicate<TaskInstance>() {
        @Override
        public boolean test(TaskInstance taskInstance) {
            return taskInstance.getStatus().equals(TaskInstanceStatus.COMPLETED);
        }
    };


    public void instantiateJob() {

        Iterable<Process> processes = processRepository.findAllByProcessStatus(ProcessStatus.AVAILABLE);

        processes.forEach(process -> {
            BaseProcessAction baseProcessAction = (BaseProcessAction)applicationContext.getBean(process.getInstantiationActionBean());
            baseProcessAction.instantiate(process);
        });

    }

    public void findAndMarkReady(final int maximumActiveProcess) {
        long activeCount = processArangoRepository.countByStatus(ProcessInstanceStatus.INPROGRESS);
        if (activeCount < maximumActiveProcess) {
            Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.PENDING, ProcessInstanceEvent.DEPENDENCY_RESOLVED);
            optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
                Criteria<ProcessInstance> processInstanceCriteria = foundProcessInstance.getExecutionCriteria();
                boolean output = true;
                if (processInstanceCriteria != null)
                    output = processInstanceCriteria.evaluate(foundProcessInstance, arangoOperations).isCriteriaResult();
                if (output)
                    processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()), ProcessInstanceEvent.DEPENDENCY_RESOLVED);
                else
                    processEventManager.rollbackEvent(UUID.fromString(foundProcessInstance.getArangoKey()), ProcessInstanceEvent.DEPENDENCY_RESOLVED, arangoOperations);
            }, () -> log.debug("No pending process instance found"));
        } else {
            log.debug("Maximum number of process are already running");
        }
    }

    @EventListener(condition = "@processManager.isReady.test(#processInstance)")
    public void startProcess(ProcessInstance processInstance) {
            Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.READY,ProcessInstanceEvent.PICKEDUP);
            optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
                processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.PICKEDUP);
            }, ()-> log.debug("No ready process instance found"));

    }

    @EventListener(condition = "@processManager.isTaskCompleted.test(#taskInstance)")
    public void completeProcess(TaskInstance taskInstance){
        Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findCompletedProcessInstance(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.INPROGRESS,ProcessInstanceEvent.FINISHED);
        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.FINISHED);
            }, ()-> log.debug("No process instance found to be completed"));
    }

    @EventListener(condition = "@processManager.isProcessInProgress.test(#processInstance)")
    public void completeProcess(ProcessInstance processInstance){
        Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findCompletedProcessInstance(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.INPROGRESS,ProcessInstanceEvent.FINISHED);
        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.FINISHED);
        }, ()-> log.debug("No process instance found to be completed"));
    }

    public void removeAllInstancesByProcessCode(final String processCode){
        Process process = processRepository.findByProcessCode(processCode).orElseThrow(NotFoundException::new);
        ProcessInstance processTemplate = processInstanceRepository.findTemplateByProcess(process.getArangoId()).orElseThrow(NotFoundException::new);

       List<ProcessInstance> processInstances = processInstanceRepository.findAllByProcessTemplate(processTemplate.getArangoId());
        if (processInstances != null && !processInstances.isEmpty()){
            processInstances.forEach(processInstance -> {
                deleteProcessInstance(processInstance);
            });
        }
    }

    public void deleteProcessInstance(final ProcessInstance processInstance){
        if(processInstance.isTemplate())
            taskInstanceRepository.deleteAllByProcessTemplate(processInstance.getArangoId());
        else
            taskInstanceRepository.deleteAllByProcessInstance(processInstance.getArangoId());
        processInstanceRepository.delete(processInstance);
    }

}
