package com.github.amag.processorchestrator.task.executor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.task.types.SimpleAction;
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

    public void execute(SimpleAction simpleAction, UUID taskInstanceId) {
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);
        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            SimpleAction managedActionBean = null;
            try{
                managedActionBean = applicationContext.getBean(simpleAction.getClass());
            } catch(NoSuchBeanDefinitionException e){
                log.debug("Bean not found for {} using from object",simpleAction);
            }

            if(managedActionBean != null){
                simpleAction.updateManagedBeanProperties(managedActionBean);
                Object output =  managedActionBean.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
                taskInstance.setOutput(output);
                arangoOperations.repsert(taskInstance);
            } else {
                Object output =  simpleAction.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
                taskInstance.setOutput(output);
                arangoOperations.repsert(taskInstance);
            }
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

}
