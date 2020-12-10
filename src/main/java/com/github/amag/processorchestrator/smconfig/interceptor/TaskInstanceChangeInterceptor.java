package com.github.amag.processorchestrator.smconfig.interceptor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.platform.domain.ErrorLog;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.TransitionLog;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class TaskInstanceChangeInterceptor extends StateMachineInterceptorAdapter<TaskInstanceStatus, TaskInstanceEvent> {

    private final ArangoOperations arangoOperations;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void postStateChange(State<TaskInstanceStatus, TaskInstanceEvent> state, Message<TaskInstanceEvent> message, Transition<TaskInstanceStatus, TaskInstanceEvent> transition, StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER, " ")))
                .ifPresent(taskInstanceId -> {
                        log.debug("Saving state for order id: "+taskInstanceId+" Status: "+state.getId());
                            TaskInstance taskInstance = arangoOperations.find(UUID.fromString(taskInstanceId), TaskInstance.class).get();
                            TransitionLog transitionLog = TransitionLog.builder()
                                    .entityType(TaskInstance.class.getSimpleName())
                                    .entityId(stateMachine.getUuid())
                                    .fromState(taskInstance.getStatus().toString())
                                    .toState(state.getId().toString())
                                    .build();
                            arangoOperations.insert(transitionLog);
                            taskInstance.setStatus(state.getId());
                        arangoOperations.repsert(taskInstance);
                        applicationEventPublisher.publishEvent(taskInstance);
                }
                );


    }

    public Exception stateMachineError(StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine, Exception exception) {
        stateMachine.getExtendedState().getVariables().put("ERROR", exception);
        log.error("StateMachineError", exception);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        ErrorLog errorLog = ErrorLog.builder()
                .entityId(stateMachine.getUuid())
                .entityType(TaskInstance.class.getSimpleName())
                .stackTrace(sw.toString())
                .build();
        arangoOperations.insert(errorLog);
        return exception;
    }

}
