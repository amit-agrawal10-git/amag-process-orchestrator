package com.github.amag.processorchestrator.interceptor;

import com.github.amag.processorchestrator.config.TaskInstanceStateMachineConfig;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class TaskInstanceChangeInterceptor extends StateMachineInterceptorAdapter<TaskInstanceStatus, TaskInstanceEvent> {

    private final TaskInstanceRepository taskInstanceRepository;

    @Override
    @Transactional
    public void preStateChange(State<TaskInstanceStatus, TaskInstanceEvent> state, Message<TaskInstanceEvent> message, Transition<TaskInstanceStatus, TaskInstanceEvent> transition, StateMachine<TaskInstanceStatus, TaskInstanceEvent> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(TaskInstanceStateMachineConfig.TASK_INSTANCE_ID_HEADER, " ")))
                .ifPresent(taskInstanceId -> {
                        log.debug("Saving state for order id: "+taskInstanceId+" Status: "+state.getId());
                            TaskInstance taskInstance = taskInstanceRepository.findById(UUID.fromString(taskInstanceId)).get();
                        taskInstance.setStatus(state.getId());
                        taskInstanceRepository.save(taskInstance);
                }
                );
    }
}
