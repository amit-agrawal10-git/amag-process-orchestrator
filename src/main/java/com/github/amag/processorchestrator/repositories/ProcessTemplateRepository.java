package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessTemplate;

import java.util.UUID;

public interface ProcessTemplateRepository  extends ArangoRepository<ProcessTemplate, UUID> {
}
