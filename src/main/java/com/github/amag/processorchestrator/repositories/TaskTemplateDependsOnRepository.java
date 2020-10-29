package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateDependsOnTaskTemplate;

import java.util.UUID;

public interface TaskTemplateDependsOnRepository  extends ArangoRepository<TaskTemplateDependsOnTaskTemplate, UUID> {

}
