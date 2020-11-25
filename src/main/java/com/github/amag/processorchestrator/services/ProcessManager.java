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
import com.github.amag.processorchestrator.process.actions.BaseProcessAction;
import com.github.amag.processorchestrator.repositories.ProcessArangoRepository;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.events.ProcessEventManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessManager {

    private final ProcessArangoRepository processArangoRepository;
    private final ProcessRepository processRepository;
    private final ProcessEventManager processEventManager;
    private final ApplicationContext applicationContext;
    private final ArangoOperations arangoOperations;

    public void instantiateJob() {

        Iterable<Process> processes = processRepository.findAll();

        processes.forEach(process -> {
            BaseProcessAction baseProcessAction = (BaseProcessAction)applicationContext.getBean(process.getInstantiationActionBean());
            baseProcessAction.execute(process);
        });

    }

    public void findAndMarkReady() {
        Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.PENDING,ProcessInstanceEvent.DEPENDENCY_RESOLVED);
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
        long activeCount = processArangoRepository.countByStatus(ProcessInstanceStatus.INPROGRESS);
        if (activeCount < maximumActiveProcess) {
            Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findByStatusAndIsTemplate(ProcessInstanceStatus.READY,ProcessInstanceEvent.PICKEDUP);
            optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
                processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.PICKEDUP);
            }, ()-> log.debug("No ready process instance found"));
        } else {
            log.debug("Maximum number of process are already running");
        }
    }

    public void completeProcess(){
        Optional<ProcessInstance> optionalProcessInstance = processArangoRepository.findCompletedProcessInstance(TaskInstanceStatus.COMPLETED, ProcessInstanceStatus.INPROGRESS,ProcessInstanceEvent.FINISHED);
        optionalProcessInstance.ifPresentOrElse(foundProcessInstance -> {
            processEventManager.sendProcessInstanceEvent(UUID.fromString(foundProcessInstance.getArangoKey()),ProcessInstanceEvent.FINISHED);
            }, ()-> log.debug("No process instance found to be completed"));
    }


}
