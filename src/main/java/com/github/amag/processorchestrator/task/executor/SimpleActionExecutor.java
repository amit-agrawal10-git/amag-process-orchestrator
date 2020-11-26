package com.github.amag.processorchestrator.task.executor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.task.types.BaseTaskAction;
import com.github.amag.processorchestrator.task.types.SimpleTaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class SimpleActionExecutor implements TaskActionExecutor {

    private final ArangoOperations arangoOperations;
    private final ApplicationContext applicationContext;

    public void execute(BaseTaskAction baseTaskAction, UUID taskInstanceId) {
        SimpleTaskAction simpleTaskAction = (SimpleTaskAction)baseTaskAction;
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            SimpleTaskAction managedActionBean = applicationContext.getBean(simpleTaskAction.getClass());
            simpleTaskAction.updateManagedBeanProperties(managedActionBean);
            Object output =  managedActionBean.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
            taskInstance.setOutput(output);
            arangoOperations.repsert(taskInstance);
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

    public void rollback(BaseTaskAction baseTaskAction, UUID taskInstanceId) {
        SimpleTaskAction simpleTaskAction = (SimpleTaskAction)baseTaskAction;
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            SimpleTaskAction managedActionBean = applicationContext.getBean(simpleTaskAction.getClass());
            simpleTaskAction.updateManagedBeanProperties(managedActionBean);
            managedActionBean.rollback(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

}
