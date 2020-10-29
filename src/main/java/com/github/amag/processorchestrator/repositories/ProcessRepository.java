package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

public interface ProcessRepository extends ArangoRepository<Process, UUID> {
    Iterable<Process> findAllByExecutedUptoBefore(Date date);
}
