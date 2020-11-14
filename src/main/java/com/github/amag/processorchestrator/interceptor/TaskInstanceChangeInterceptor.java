package com.github.amag.processorchestrator.interceptor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.ErrorLog;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.TransitionLog;
import com.github.amag.processorchestrator.domain.enums.EntityType;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.smconfig.TaskInstanceStateMachineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final TaskInstanceRepository taskInstanceRepository;
    private final ArangoOperations arangoOperations;

    @Override
    @Transactional
    public void postStateChange(State<TaskInstanceStatus, TaskInstanceEvent> state, Message<TaskInstanceEvent> message, Transition<TaskInstanceStatus, TaskInstanceEvent> transition, StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER, " ")))
                .ifPresent(taskInstanceId -> {
                        log.debug("Saving state for order id: "+taskInstanceId+" Status: "+state.getId());
                            TaskInstance taskInstance = taskInstanceRepository.findById(UUID.fromString(taskInstanceId)).get();
                            TransitionLog transitionLog = TransitionLog.builder()
                                    .entityType(EntityType.TASK_INSTANCE)
                                    .entityId(stateMachine.getUuid())
                                    .fromState(taskInstance.getStatus().toString())
                                    .toState(state.getId().toString())
                                    .build();
                            arangoOperations.insert(transitionLog);
                            taskInstance.setStatus(state.getId());
                        taskInstanceRepository.save(taskInstance);
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
                .entityType(EntityType.TASK_INSTANCE)
                .stackTrace(sw.toString())
                .build();
        arangoOperations.insert(errorLog);
        return exception;
    }



}
