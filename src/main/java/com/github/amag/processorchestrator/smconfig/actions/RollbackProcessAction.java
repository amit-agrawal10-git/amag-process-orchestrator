package com.github.amag.processorchestrator.smconfig.actions;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.services.TaskManager;
import com.github.amag.processorchestrator.smconfig.ProcessInstanceStateMachineConfig;
import com.github.amag.processorchestrator.smconfig.events.TaskEventManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class RollbackProcessAction implements Action<ProcessInstanceStatus, ProcessInstanceEvent> {

    private final ArangoOperations arangoOperations;
    private final TaskManager taskManager;
    private final ApplicationContext applicationContext;

    @Override
    public void execute(StateContext<ProcessInstanceStatus, ProcessInstanceEvent> stateContext) {
        log.debug("start process instance was called");
        UUID instanceId = UUID.fromString(stateContext.getMessageHeader(ProcessInstanceStateMachineConfig.PROCESS_INSTANCE_ID_HEADER).toString());
        Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(instanceId,ProcessInstance.class);

        optionalProcessInstance.ifPresentOrElse(instance -> {
                    List<TaskInstance> taskInstances = taskManager.findAllTaskInstancesByProcessInstanceAndStatusIn(instance.getArangoId(), Set.of(TaskInstanceStatus.COMPLETED, TaskInstanceStatus.FAILED));
                    if (taskInstances != null){
                        TaskEventManager taskEventManager = applicationContext.getBean(TaskEventManager.class);
                        taskInstances.forEach(taskInstance -> taskEventManager.sendTaskInstanceEvent(UUID.fromString(taskInstance.getArangoKey()), TaskInstanceEvent.ROLLED_BACK));
                    }
            }, () -> log.error("Process Instance Not Found Id: {}", instanceId)
        );
    }
}
