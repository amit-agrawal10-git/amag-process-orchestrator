package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessStatus;

import java.util.UUID;

public interface ProcessStatusRepository extends ArangoRepository<ProcessStatus, UUID> {
}
