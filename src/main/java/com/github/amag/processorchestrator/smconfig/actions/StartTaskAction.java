package com.github.amag.processorchestrator.smconfig.actions;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.services.TaskManager;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.task.executor.SimpleActionExecutor;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartTaskAction implements Action<TaskInstanceStatus, TaskInstanceEvent> {

    private final TaskInstanceRepository taskInstanceRepository;
    private final SimpleActionExecutor simpleActionExecutor;

    @Override
    public void execute(StateContext<TaskInstanceStatus, TaskInstanceEvent> stateContext) {
        log.debug("start task instance was called");
        UUID taskInstanceId = UUID.fromString(stateContext.getMessageHeader(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER).toString());
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findById(taskInstanceId);

        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
                    try {
                        Class clazz = Class.forName(taskInstance.getTaskTemplate().getInstanceClass());
                        Object object = clazz.getDeclaredConstructor().newInstance();
                        if (object instanceof SimpleAction) {
                            simpleActionExecutor.execute((SimpleAction) object, taskInstanceId);
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException exception) {
                        log.error(exception.getMessage(),exception);
                        stateContext.getStateMachine().
                                getExtendedState().getVariables().put("ERROR", exception);
                    }
                }, () ->
                        log.error("Task Instance Not Found Id: " + taskInstanceId)
        );
    }
}