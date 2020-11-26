package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository extends ArangoRepository<Process, UUID> {
    Optional<Process> findByProcessCode(String code);
    List<Process> findAllByProcessStatus(ProcessStatus processStatus);
}
