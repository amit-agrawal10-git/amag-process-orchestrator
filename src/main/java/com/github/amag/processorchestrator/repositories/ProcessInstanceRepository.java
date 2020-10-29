package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;

import java.util.Date;
import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {
}
