package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessTaskTemplate;
import com.github.amag.processorchestrator.domain.TaskTemplate;

import java.util.UUID;

public interface ProcessTaskTemplateRepository extends ArangoRepository<ProcessTaskTemplate, UUID> {
}
