package com.github.amag.processorchestrator.smconfig.actions;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.task.executor.SimpleActionExecutor;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class RollbackTaskAction implements Action<TaskInstanceStatus, TaskInstanceEvent> {

    private final ArangoOperations arangoOperations;
    private final SimpleActionExecutor simpleActionExecutor;

    @Override
    @Transactional
    public void execute(StateContext<TaskInstanceStatus, TaskInstanceEvent> stateContext) {
        log.debug("start task instance was called");
        UUID taskInstanceId = UUID.fromString(stateContext.getMessageHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER).toString());
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId,TaskInstance.class);

        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            Object object = taskInstance.getTaskTemplate().getBaseAction();
            if (object instanceof SimpleAction) {
                try {
                    simpleActionExecutor.rollback((SimpleAction) object, taskInstanceId);
                } catch (Exception ex){
                    stateContext.getStateMachine().setStateMachineError(ex);
                    throw ex;
                }
            }
            }, () -> log.error("Task Instance Not Found Id: " + taskInstanceId)
        );
    }
}
