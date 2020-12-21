package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {

    @Query(" for r in #collection filter r.status == @status filter r.isTemplate == false #pageable return r ")
    Page<ProcessInstance> findAllInstancesByStatus(@Param("status") ProcessInstanceStatus status, Pageable pageable);

    @Query(" for r in #collection filter r.isTemplate == true #pageable return r ")
    Page<ProcessInstance> findAllTemplates(Pageable pageable);

    @Query(" for r in #collection filter r.isTemplate == false #pageable return r ")
    Page<ProcessInstance> findAllInstances(Pageable pageable);

    @Query(" for r in #collection filter r.process == @processId filter r.isTemplate == true return r ")
    Optional<ProcessInstance> findTemplateByProcess(@Param("processId") String processId);

    @Query(" for r in #collection filter r.processTemplate == @processTemplate filter r.isTemplate == false #pageable return r ")
    Page<ProcessInstance> findAllInstancesByProcessTemplate(@Param("processTemplate") String processTemplate, Pageable pageable);

    @Query(" for r in #collection filter r.processTemplate == @processTemplate filter r.isTemplate == false return r ")
    List<ProcessInstance> findAllByProcessTemplate(@Param("processTemplate") String processTemplateId);

    @Query(" for r in #collection filter r.processTemplate == @processTemplate filter r.isTemplate == false filter r.status == @status #pageable return r ")
    Page<ProcessInstance> findAllInstancesByStatusAndProcessTemplate(@Param("status") String status,@Param("processTemplate") String processTemplate, Pageable pageable);


}
