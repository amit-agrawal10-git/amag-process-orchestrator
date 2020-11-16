package com.github.amag.processorchestrator.smconfig;

import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "taskInstanceStateMachineFactory")
@Slf4j
@RequiredArgsConstructor
public class TaskInstanceStateMachineConfig extends StateMachineConfigurerAdapter<TaskInstanceStatus, TaskInstanceEvent> {

    public static final String TASK_INSTANCE_ID_HEADER = "taskInstanceId";
    private final Action<TaskInstanceStatus, TaskInstanceEvent> startTaskAction;
    private final StateMachineListenerAdapter<TaskInstanceStatus, TaskInstanceEvent> taskInstanceListener;

    @Override
    public void configure(StateMachineStateConfigurer<TaskInstanceStatus, TaskInstanceEvent> states) throws Exception {
        states.withStates()
                .initial(TaskInstanceStatus.PENDING)
                .states(EnumSet.allOf(TaskInstanceStatus.class))
                .end(TaskInstanceStatus.FAILED)
                .end(TaskInstanceStatus.COMPLETED);

    }

    @Override
    public void configure(StateMachineTransitionConfigurer<TaskInstanceStatus, TaskInstanceEvent> transitions) throws Exception {
        transitions.withExternal()
                    .source(TaskInstanceStatus.PENDING)
                    .target(TaskInstanceStatus.READY)
                    .event(TaskInstanceEvent.DEPENDENCY_RESOLVED)

                .and().withExternal()
                    .source(TaskInstanceStatus.READY)
                    .target(TaskInstanceStatus.STARTED)
                    .event(TaskInstanceEvent.PICKEDUP)

                .and().withExternal()
                    .source(TaskInstanceStatus.STARTED)
                    .target(TaskInstanceStatus.COMPLETED)
                    .event(TaskInstanceEvent.FINISHED)
                    .action(startTaskAction)
                    .guard(instanceIdGuard())

                .and().withExternal()
                    .source(TaskInstanceStatus.CALLEDBACK)
                    .target(TaskInstanceStatus.COMPLETED)
                    .event(TaskInstanceEvent.FINISHED)

                .and().withExternal()
                    .source(TaskInstanceStatus.READY)
                    .target(TaskInstanceStatus.FAILED)
                    .event(TaskInstanceEvent.ERROR_OCCURRED)

                .and().withExternal()
                    .source(TaskInstanceStatus.STARTED)
                    .target(TaskInstanceStatus.FAILED)
                    .event(TaskInstanceEvent.ERROR_OCCURRED)

                .and().withExternal()
                    .source(TaskInstanceStatus.WAITING)
                    .target(TaskInstanceStatus.FAILED)
                    .event(TaskInstanceEvent.ERROR_OCCURRED)

                .and().withExternal()
                    .source(TaskInstanceStatus.CALLEDBACK)
                    .target(TaskInstanceStatus.FAILED)
                    .event(TaskInstanceEvent.ERROR_OCCURRED);

    }

    public Guard<TaskInstanceStatus, TaskInstanceEvent> instanceIdGuard(){
        return stateContext -> {
          return stateContext.getMessageHeader(TASK_INSTANCE_ID_HEADER) != null;
        };
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<TaskInstanceStatus, TaskInstanceEvent> config) throws Exception {
       config.withConfiguration()
               .listener(taskInstanceListener);
    }
}
