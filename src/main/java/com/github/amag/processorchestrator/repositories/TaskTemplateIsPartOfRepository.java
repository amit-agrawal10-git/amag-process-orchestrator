package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateIsPartOfProcessTemplate;

import java.util.UUID;

public interface TaskTemplateIsPartOfRepository  extends ArangoRepository<TaskTemplateIsPartOfProcessTemplate, UUID> {
}
