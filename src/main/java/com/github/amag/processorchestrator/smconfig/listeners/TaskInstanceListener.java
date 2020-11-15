package com.github.amag.processorchestrator.smconfig.listeners;

import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskInstanceListener extends StateMachineListenerAdapter<TaskInstanceStatus, TaskInstanceEvent> {
    @Override
    public void eventNotAccepted(Message<TaskInstanceEvent> event) {
        log.debug("{} not accepted",event);
    }


}
