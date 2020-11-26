package com.github.amag.processorchestrator.task.executor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.task.types.BaseTaskAction;
import com.github.amag.processorchestrator.task.types.ParallelTaskAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

@RequiredArgsConstructor
@Slf4j
@Component
public class ParallelActionExecutor implements TaskActionExecutor {

    private final ArangoOperations arangoOperations;
    private final ApplicationContext applicationContext;
    private final ParallelAsyncTaskExecutor parallelAsyncTaskExecutor;

    public void execute(BaseTaskAction baseTaskAction, UUID taskInstanceId) {
        log.debug("Inside execute for {}, UUID {}",baseTaskAction,taskInstanceId);
        ParallelTaskAction parallelTaskAction = (ParallelTaskAction)baseTaskAction;
        ParallelTaskAction managedActionBean = applicationContext.getBean(parallelTaskAction.getClass());
        parallelTaskAction.updateManagedBeanProperties(managedActionBean);

        List<Future<Object>> futureList = new ArrayList<>();
        Iterable<? extends Object> objects = managedActionBean.preAction(taskInstanceId);
        for (Object o:objects) {
            futureList.add(parallelAsyncTaskExecutor.execute(managedActionBean,o,taskInstanceId));
        }
        checkForCompletion(futureList);
        managedActionBean.postAction(futureList, taskInstanceId);
    }

    private void checkForCompletion(Iterable<Future<Object>> futureList){
        log.debug("checking for completion of all async tasks");
        boolean result = true;
        for (Future<Object> future:futureList) {
            if(!future.isDone())
            {
                result = false;
                break;
            }
        }
        if(!result){
            try {
                log.debug("Sleeping for {} milliseconds",1000);
                Thread.sleep(1000);
                checkForCompletion(futureList);
            } catch (InterruptedException e) {
                log.error(e.getMessage(),e);
                throw new RuntimeException(e.getMessage(),e);
            }
        }
    }

    public void rollback(BaseTaskAction baseTaskAction, UUID taskInstanceId) {
        ParallelTaskAction parallelTaskAction = (ParallelTaskAction)baseTaskAction;
        TaskInstance taskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class).get();
        ParallelTaskAction managedActionBean = applicationContext.getBean(parallelTaskAction.getClass());
        parallelTaskAction.updateManagedBeanProperties(managedActionBean);
        managedActionBean.rollback(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
    }

}
