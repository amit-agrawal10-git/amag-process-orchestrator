package com.github.amag.processorchestrator.task.types;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.task.executor.ParallelActionExecutor;
import com.github.amag.processorchestrator.task.executor.TaskActionExecutor;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public abstract class ParallelTaskAction implements BaseTaskAction {

    public abstract Iterable<? extends Object> preAction(UUID taskInstanceId);
    public abstract Object execute(Object task,UUID taskInstanceId);
    public abstract Object postAction(Iterable<Future<Object>> results,UUID taskInstanceId);

    @Override
    public void updateManagedBeanProperties(BaseTaskAction managedBean) {

    }

    @Override
    public TaskActionExecutor getTaskActionExecutor(ApplicationContext applicationContext) {
        return (ParallelActionExecutor)applicationContext.getBean("parallelActionExecutor");
    }

    public void rollback(UUID taskInstanceId, ArangoOperations arangoOperations){
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId,TaskInstance.class);
        optionalTaskInstance.ifPresent(taskInstance -> {
            taskInstance.setSentEvents(null);
            arangoOperations.repsert(taskInstance);
        });
    }

}
