package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository extends ArangoRepository<Process, UUID> {

    @Query(" for r in #collection filter r.processCode == @processCode return r ")
    Optional<Process> findByProcessCode(@Param("processCode") String code);

    @Query(" for r in #collection filter r.processStatus == @status return r ")
    List<Process> findAllByProcessStatus(@Param("status") ProcessStatus processStatus);

}
