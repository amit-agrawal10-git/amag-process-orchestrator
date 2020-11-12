package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskInstanceRepository extends ArangoRepository<TaskInstance, UUID> {

    @Query("FOR e1 IN task_instances \n" +
            "     FILTER e1.isTemplate == true  \n" +
            "     FILTER e1.processTemplate == @processTemplateArangoId\n" +
            "     FILTER e1._id not in \n" +
            "    (FOR e IN task_instances \n" +
            "     FILTER e.isTemplate == true  \n" +
            "     FILTER e.dependsOn != NULL\n" +
            "     FILTER e1.processTemplate == e.processTemplate  \n" +
            "  RETURN e.dependsOn[0]\n" +
            "  )\n" +
            "    RETURN e1")
    List<TaskInstance> findLastTaskInstancesByProcessTemplateArangoId(@Param("processTemplateArangoId") String processTemplateArangoId);

    @Query("FOR t IN task_instances \n" +
            "  FOR p IN process_instances\n" +
            "     FILTER t.status == @currentTaskStatus \n" +
            "     FILTER t.isTemplate == false \n" +
            "     FILTER p.isTemplate == false \n" +
            "     FILTER t.processInstance == p._id\n" +
            "     FILTER p.status == @currentProcessStatus\n" +
            "     FILTER t.dependsOn ALL IN (FOR X IN task_instances FILTER X.processInstance == t.processInstance \n" +
            "     FILTER X.status == @taskDependsOnStatus \n" +
            "     return X._id) OR t.dependsOn == NULL\n" +
            "RETURN t")
    List<TaskInstance> findTaskInstanceToStart(@Param("currentTaskStatus") TaskInstanceStatus currentTaskStatus,
                                     @Param("currentProcessStatus") ProcessInstanceStatus currentProcessStatus,
                                     @Param("taskDependsOnStatus") TaskInstanceStatus taskDependsOnStatus);

    @Query("for i in task_instances \n" +
            " filter i.status == @from " +
            " filter i.isTemplate == false " +
            " sort i.processInstance " +
            " limit @limit \n" +
            " update i with {status : @to} in task_instances")
    void updateStatusFromTo(@Param("from") TaskInstanceStatus from,@Param("to") TaskInstanceStatus to, Integer limit);

    List<TaskInstance> findByStatus(TaskInstanceStatus taskInstanceStatus);
    long countByStatus(TaskInstanceStatus taskInstanceStatus);

}
