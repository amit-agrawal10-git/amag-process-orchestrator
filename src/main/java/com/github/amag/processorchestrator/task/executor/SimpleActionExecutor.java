package com.github.amag.processorchestrator.task.executor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.task.types.SimpleTaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class SimpleActionExecutor {

    private final ArangoOperations arangoOperations;
    private final ApplicationContext applicationContext;

    public void execute(SimpleTaskAction simpleTaskAction, UUID taskInstanceId) {
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            SimpleTaskAction managedActionBean = null;
            try{
                managedActionBean = applicationContext.getBean(simpleTaskAction.getClass());
            } catch(NoSuchBeanDefinitionException e){
                log.debug("Bean not found for {} using from object", simpleTaskAction);
            }
            Object output = null;
            if(managedActionBean != null){
                simpleTaskAction.updateManagedBeanProperties(managedActionBean);
                output =  managedActionBean.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
            } else {
                output =  simpleTaskAction.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
            }
            taskInstance.setOutput(output);
            arangoOperations.repsert(taskInstance);
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

    public void rollback(SimpleTaskAction simpleTaskAction, UUID taskInstanceId) {
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            SimpleTaskAction managedActionBean = null;
            try{
                managedActionBean = applicationContext.getBean(simpleTaskAction.getClass());
            } catch(NoSuchBeanDefinitionException e){
                log.debug("Bean not found for {} using from object", simpleTaskAction);
            }
            if(managedActionBean != null){
                simpleTaskAction.updateManagedBeanProperties(managedActionBean);
                managedActionBean.rollback(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
            } else {
                simpleTaskAction.rollback(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
            }
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

}
