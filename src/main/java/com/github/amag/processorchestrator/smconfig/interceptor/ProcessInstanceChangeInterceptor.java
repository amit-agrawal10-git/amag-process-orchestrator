package com.github.amag.processorchestrator.smconfig.interceptor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.platform.domain.ErrorLog;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TransitionLog;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.smconfig.ProcessInstanceStateMachineConfig;
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
public class ProcessInstanceChangeInterceptor extends StateMachineInterceptorAdapter<ProcessInstanceStatus, ProcessInstanceEvent> {

    private final ArangoOperations arangoOperations;

    @Override
    @Transactional
    public void postStateChange(State<ProcessInstanceStatus, ProcessInstanceEvent> state,
                               Message<ProcessInstanceEvent> message,
                               Transition<ProcessInstanceStatus, ProcessInstanceEvent> transition,
                               StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(ProcessInstanceStateMachineConfig.PROCESS_INSTANCE_ID_HEADER, " ")))
                .ifPresent(instanceId -> {
                        log.debug("Saving state for id: "+instanceId+" Status: "+state.getId());
                        final ProcessInstance instance = arangoOperations.find(UUID.fromString(instanceId),ProcessInstance.class).get();
                            TransitionLog transitionLog = TransitionLog.builder()
                                    .entityType(ProcessInstance.class.getSimpleName())
                                    .entityId(stateMachine.getUuid())
                                    .fromState(instance.getStatus().toString())
                                    .toState(state.getId().toString())
                                    .build();
                            arangoOperations.insert(transitionLog);

                        instance.setStatus(state.getId());
                        arangoOperations.repsert(instance);
                }
                );
    }

    public Exception stateMachineError(StateMachine<ProcessInstanceStatus, ProcessInstanceEvent> stateMachine, Exception exception) {
        stateMachine.getExtendedState().getVariables().put("ERROR", exception);
        log.error("StateMachineError", exception);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        ErrorLog errorLog = ErrorLog.builder()
                .entityId(stateMachine.getUuid())
                .entityType(ProcessInstance.class.getSimpleName())
                .stackTrace(sw.toString())
                .build();
        arangoOperations.insert(errorLog);
        return exception;
    }

}
