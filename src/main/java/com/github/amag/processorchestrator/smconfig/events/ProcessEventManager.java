package com.github.amag.processorchestrator.smconfig.events;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.smconfig.ProcessInstanceStateMachineConfig;
import com.github.amag.processorchestrator.smconfig.interceptor.ProcessInstanceChangeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProcessEventManager {

    private final ArangoOperations arangoOperations;
    private final StateMachineFactory<ProcessInstanceStatus, ProcessInstanceEvent> processInstanceStateMachineFactory;
    private final ProcessInstanceChangeInterceptor processInstanceChangeInterceptor;

    @Async(value = "procInstEx")
    public void sendProcessInstanceEvent(UUID instanceId, ProcessInstanceEvent processInstanceEvent){

        Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(instanceId,ProcessInstance.class);
        optionalProcessInstance.ifPresentOrElse(processInstance -> {
            StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine = build(processInstance);
            Message message
                    = MessageBuilder.withPayload(processInstanceEvent)
                    .setHeader(ProcessInstanceStateMachineConfig.PROCESS_INSTANCE_ID_HEADER,processInstance.getArangoKey())
                    .build();
            stateMachine.sendEvent(message);

            if(stateMachine.hasStateMachineError()){
                sendProcessInstanceEvent(UUID.fromString(processInstance.getArangoKey()), ProcessInstanceEvent.ERROR_OCCURRED);
            }

        },() -> {
            log.error("Error while sending event");
        });
    }

    public static void rollbackEvent(UUID instanceId, ProcessInstanceEvent processInstanceEvent, ArangoOperations arangoOperations){
        Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(instanceId,ProcessInstance.class);
        optionalProcessInstance.ifPresent(processInstance -> {
            Set<ProcessInstanceEvent> processInstanceEvents = processInstance.getSentEvents();
            processInstanceEvents.remove(processInstanceEvent);
            processInstance.setSentEvents(processInstanceEvents);
            arangoOperations.repsert(processInstance);
        });
    }

    private StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> build(ProcessInstance processInstance){
        StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine = processInstanceStateMachineFactory.getStateMachine(UUID.fromString(processInstance.getArangoKey()));
        stateMachine.stop();

        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(
                        sma -> {
                            sma.addStateMachineInterceptor(processInstanceChangeInterceptor);
                            sma.resetStateMachine(new DefaultStateMachineContext<>(processInstance.getStatus(), null,null,null));
                        }
                );
        stateMachine.start();
        return stateMachine;
    }
}
