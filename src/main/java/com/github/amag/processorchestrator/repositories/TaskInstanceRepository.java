package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TaskInstanceRepository extends ArangoRepository<TaskInstance, UUID> {

    @Query(" for r in #collection filter r.processInstance == @processInstanceId filter r.isTemplate == false #pageable return r ")
    Page<TaskInstance> findAllInstancesByProcessInstance(@Param("processInstanceId") String processInstanceId, Pageable pageable);

    @Query(" for r in #collection filter r.processTemplate == @processTemplateId filter r.isTemplate == true #pageable return r ")
    Page<TaskInstance> findAllTemplateByProcessTemplate(@Param("processTemplateId") String processTemplateId, Pageable pageable);

    @Query(" for r in #collection filter r.processTemplate == @processTemplateId filter r.isTemplate == true return r ")
    List<TaskInstance> findAllTemplatesByProcessTemplate(@Param("processTemplateId") String processTemplateId);

    @Query(" for r in #collection filter r.processInstance == @processInstanceId filter r.status in @taskInstanceStatues filter r.isTemplate == false return r ")
    List<TaskInstance> findAllByProcessInstanceAndStatusIn(@Param("processInstanceId") String processInstanceId,@Param("taskInstanceStatues") Set<TaskInstanceStatus> taskInstanceStatuses);

    @Query(" for r in #collection filter r.processInstance == @processInstanceId filter r.isTemplate == false remove r in #collection ")
    void deleteAllByProcessInstance(@Param("processInstanceId") String processInstanceId);

    @Query(" for r in #collection filter r.processTemplate == @processTemplateId filter r.isTemplate == true remove r in #collection ")
    void deleteAllByProcessTemplate(@Param("processTemplateId") String processTemplateId);
}
