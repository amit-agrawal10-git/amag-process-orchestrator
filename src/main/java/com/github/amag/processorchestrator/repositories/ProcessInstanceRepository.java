package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository extends ArangoRepository<ProcessInstance, UUID> {

    Optional<ProcessInstance> findByStatusAndIsTemplate(ProcessInstanceStatus processInstanceStatus,boolean IsTemplate);

    @Query("for p in process_instances \n" +
            "   filter p.isTemplate == false \n" +
            "   filter p.status != @currentProcessStatus\n" +
            "   filter (\n" +
            "   for t in task_instances \n" +
            "   filter t.isTemplate == false \n" +
            "   filter t.processInstance == p._id\n" +
            "   return t.status) ALL IN [@currentTaskStatus]\n" +
            "   limit 1 return p")
    Optional<ProcessInstance> findCompletedProcessInstance(@Param("currentTaskStatus") TaskInstanceStatus currentTaskStatus,
                                               @Param("currentProcessStatus") ProcessInstanceStatus currentProcessStatus);

}
