package com.github.amag.processorchestrator.task.executor;

import com.github.amag.processorchestrator.task.types.BaseTaskAction;

import java.util.UUID;

public interface TaskActionExecutor {
    void execute(BaseTaskAction baseTaskAction, UUID taskInstanceId);
    void rollback(BaseTaskAction baseTaskAction, UUID taskInstanceId);
}
