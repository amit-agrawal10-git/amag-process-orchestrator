package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.TaskTemplate;

import java.util.UUID;

public interface TaskTemplateRepository  extends ArangoRepository<TaskTemplate, UUID> {
}
