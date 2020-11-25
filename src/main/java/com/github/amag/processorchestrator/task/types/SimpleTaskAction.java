package com.github.amag.processorchestrator.task.types;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;

import java.util.Optional;
import java.util.UUID;

public abstract class SimpleTaskAction implements BaseTaskAction {

    public abstract Object execute(UUID taskInstanceId, ArangoOperations arangoOperations);

    @Override
    public void updateManagedBeanProperties(BaseTaskAction managedBean) {

    }

    public void rollback(UUID taskInstanceId, ArangoOperations arangoOperations){
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId,TaskInstance.class);
        optionalTaskInstance.ifPresent(taskInstance -> {
            taskInstance.setSentEvents(null);
            arangoOperations.repsert(taskInstance);
        });
    }

}
