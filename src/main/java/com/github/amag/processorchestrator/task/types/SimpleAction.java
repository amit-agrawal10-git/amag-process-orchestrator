package com.github.amag.processorchestrator.task.types;

import com.arangodb.springframework.core.ArangoOperations;

import java.util.UUID;

public interface SimpleAction extends BaseAction {

    public Object execute(UUID taskInstanceId, ArangoOperations arangoOperations);

}
