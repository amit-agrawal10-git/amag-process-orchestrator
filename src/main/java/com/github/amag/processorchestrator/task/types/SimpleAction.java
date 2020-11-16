package com.github.amag.processorchestrator.task.types;

import com.arangodb.springframework.core.ArangoOperations;

import java.util.UUID;

public abstract class SimpleAction implements BaseAction {

    public abstract Object execute(UUID taskInstanceId, ArangoOperations arangoOperations);

    @Override
    public void updateManagedBeanProperties(BaseAction managedBean) {

    }
}
