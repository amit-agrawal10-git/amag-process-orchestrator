package com.github.amag.processorchestrator.smconfig.actions;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.task.types.BaseTaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartTaskAction implements Action<TaskInstanceStatus, TaskInstanceEvent> {

    private final ArangoOperations arangoOperations;
    private final ApplicationContext applicationContext;

    @Override
    public void execute(StateContext<TaskInstanceStatus, TaskInstanceEvent> stateContext) {
        log.debug("start task instance was called");
        UUID taskInstanceId = UUID.fromString(stateContext.getMessageHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER).toString());
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId,TaskInstance.class);

        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            BaseTaskAction baseTaskAction = (taskInstance.getBaseTaskAction()==null)?taskInstance.getTaskTemplate().getBaseTaskAction():taskInstance.getBaseTaskAction();
                try {
                    baseTaskAction.getTaskActionExecutor(applicationContext).execute(baseTaskAction,taskInstanceId);
                } catch (Exception ex){
                    stateContext.getStateMachine().setStateMachineError(ex);
                    throw ex;
                }
            }, () ->
                log.error("Task Instance Not Found Id: " + taskInstanceId)
        );
    }
}
