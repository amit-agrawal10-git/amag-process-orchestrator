package com.github.amag.processorchestrator.task.executor;

import com.github.amag.processorchestrator.task.types.ParallelTaskAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Future;

@Slf4j
@Component
public class ParallelAsyncTaskExecutor {

    @Async(value = "taskInstEx")
    public Future<Object> execute(ParallelTaskAction parallelTaskAction, Object task, UUID taskInstanceId) {
      return new AsyncResult<Object>(parallelTaskAction.execute(task, taskInstanceId));
    }

}
