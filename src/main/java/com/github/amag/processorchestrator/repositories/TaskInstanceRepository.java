package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.TaskInstance;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskInstanceRepository extends ArangoRepository<TaskInstance, UUID> {

    @Query("WITH process_templates FOR e IN task_instances FILTER e.processTemplate == @processTemplateArangoId AND e.isTemplate == true  RETURN e")
    List<TaskInstance> findAllByProcessTemplateArangoIdAndIsTemplateTrue(@Param("processTemplateArangoId") String processTemplateArangoId);

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

}
