package com.github.amag.processorchestrator.smconfig;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.criteria.Criteria;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Proc;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

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

                    .source(ProcessInstanceStatus.READY)
                    .target(ProcessInstanceStatus.INPROGRESS)
                    .event(ProcessInstanceEvent.PICKEDUP)
                    .action(startProcessAction)
                    .guard(instanceIdGuard())
                    .guard(executionConditionGuard())

                .and().withExternal()
                    .source(ProcessInstanceStatus.INPROGRESS)
                    .target(ProcessInstanceStatus.COMPLETED)
                    .event(ProcessInstanceEvent.FINISHED)

                .and().withExternal()
                    .source(ProcessInstanceStatus.READY)
                    .target(ProcessInstanceStatus.FAILED)
                    .event(ProcessInstanceEvent.ERROR_OCCURRED)

                .and().withExternal()
                    .source(ProcessInstanceStatus.INPROGRESS)
                    .target(ProcessInstanceStatus.FAILED)
                    .event(ProcessInstanceEvent.ERROR_OCCURRED);

    }

    public Guard<ProcessInstanceStatus, ProcessInstanceEvent> instanceIdGuard(){
        return stateContext -> {
          return stateContext.getMessageHeader(PROCESS_INSTANCE_ID_HEADER) != null;
        };
    }

    public Guard<ProcessInstanceStatus, ProcessInstanceEvent> executionConditionGuard(){
        return stateContext -> {
            final Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(UUID.fromString((String)stateContext.getMessageHeader(PROCESS_INSTANCE_ID_HEADER)),ProcessInstance.class);
            final ProcessInstance processInstance;
            boolean output = true;
            if(optionalProcessInstance.isPresent()){
                processInstance = optionalProcessInstance.get();
                Criteria<ProcessInstance> processInstanceCriteria = processInstance.getExecutionCriteria();
                if(processInstanceCriteria!=null)
                output = processInstanceCriteria.evaluate(processInstance, arangoOperations).isCriteriaResult();
            }
            return output;
        };
    }

}
