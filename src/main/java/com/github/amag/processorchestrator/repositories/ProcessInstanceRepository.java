package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {

    Optional<ProcessInstance> findByStatus(ProcessInstanceStatus processInstanceStatus);
}
