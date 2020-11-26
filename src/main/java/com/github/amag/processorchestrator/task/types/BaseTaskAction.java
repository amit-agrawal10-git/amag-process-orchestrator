package com.github.amag.processorchestrator.task.types;

import com.github.amag.processorchestrator.task.executor.TaskActionExecutor;
import org.springframework.context.ApplicationContext;

public interface BaseTaskAction {

    void updateManagedBeanProperties(BaseTaskAction managedBean);

    TaskActionExecutor getTaskActionExecutor(ApplicationContext applicationContext);

}
