package com.github.amag.processorchestrator.smconfig;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "processInstanceStateMachineFactory")
@Slf4j
@RequiredArgsConstructor
public class ProcessInstanceStateMachineConfig extends StateMachineConfigurerAdapter<ProcessInstanceStatus, ProcessInstanceEvent> {

    public static final String PROCESS_INSTANCE_ID_HEADER = "processInstanceId";
    private final ArangoOperations arangoOperations;
    private final Action<ProcessInstanceStatus, ProcessInstanceEvent> startProcessAction;

    @Override
    public void configure(StateMachineStateConfigurer<ProcessInstanceStatus, ProcessInstanceEvent> states) throws Exception {
        states.withStates()
                .initial(ProcessInstanceStatus.PENDING)
                .states(EnumSet.allOf(ProcessInstanceStatus.class))
                .end(ProcessInstanceStatus.FAILED)
                .end(ProcessInstanceStatus.COMPLETED);

    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ProcessInstanceStatus, ProcessInstanceEvent> transitions) throws Exception {
        transitions.withExternal()
                .source(ProcessInstanceStatus.PENDING)
                .target(ProcessInstanceStatus.READY)
                .event(ProcessInstanceEvent.DEPENDENCY_RESOLVED)
                .guard(instanceIdGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.READY)
                    .target(ProcessInstanceStatus.INPROGRESS)
                    .event(ProcessInstanceEvent.PICKEDUP)
                    .action(startProcessAction)
                    .guard(instanceIdGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.INPROGRESS)
                    .target(ProcessInstanceStatus.COMPLETED)
                    .event(ProcessInstanceEvent.FINISHED)
                    .guard(instanceIdGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.PENDING)
                    .target(ProcessInstanceStatus.FAILED)
                    .event(ProcessInstanceEvent.ERROR_OCCURRED)
                    .guard(instanceIdGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.READY)
                    .target(ProcessInstanceStatus.FAILED)
                    .event(ProcessInstanceEvent.ERROR_OCCURRED)
                    .guard(instanceIdGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.INPROGRESS)
                    .target(ProcessInstanceStatus.FAILED)
                    .event(ProcessInstanceEvent.ERROR_OCCURRED)
                    .guard(instanceIdGuard());

    }

    public Guard<ProcessInstanceStatus, ProcessInstanceEvent> instanceIdGuard(){
        return stateContext -> {
          return stateContext.getMessageHeader(PROCESS_INSTANCE_ID_HEADER) != null;
        };
    }

}
