package com.github.amag.processorchestrator.repositories;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class TaskInstanceRepository {

    private final ArangoOperations arangoOperations;

    public List<TaskInstance> findLastTaskInstancesByProcessTemplateArangoId(String processTemplateArangoId){
        final String query = "FOR e1 IN task_instances \n" +
                "     FILTER e1.isTemplate == true  \n" +
                "     FILTER e1.processTemplate == @processTemplateArangoId\n" +
                "     FILTER e1._id not in \n" +
                "    (FOR e IN task_instances \n" +
                "     FILTER e.isTemplate == true  \n" +
                "     FILTER e.dependsOn != NULL\n" +
                "     FILTER e1.processTemplate == e.processTemplate  \n" +
                "  RETURN e.dependsOn[0]\n" +
                "  )\n" +
                "    RETURN e1";

        final Map<String,Object> bindVariables = new MapBuilder()
                .put("processTemplateArangoId", processTemplateArangoId)
                .get();

        ArangoCursor<TaskInstance> taskInstances = arangoOperations.query(query,bindVariables,null,TaskInstance.class);
        return taskInstances.asListRemaining();
    };


    public Optional<TaskInstance> findTaskInstanceToStart(
            TaskInstanceStatus currentTaskStatus,
            ProcessInstanceStatus currentProcessStatus,
            TaskInstanceStatus taskDependsOnStatus,
            TaskInstanceEvent taskInstanceEvent){

        final String query = "FOR t IN task_instances \n" +
                "  FOR p IN process_instances\n" +
                "     FILTER t.status == @currentTaskStatus \n" +
                "     FILTER t.isTemplate == false \n" +
                "     FILTER @taskEvent not in t.sentEvents OR t.sentEvents == NULL \n" +
                "     FILTER p.isTemplate == false \n" +
                "     FILTER t.processInstance == p._id\n" +
                "     FILTER p.status == @currentProcessStatus\n" +
                "     FILTER t.dependsOn ALL IN (FOR X IN task_instances FILTER X.processInstance == t.processInstance \n" +
                "     FILTER X.status == @taskDependsOnStatus \n" +
                "     return X._id) OR t.dependsOn == NULL\n" +
                "   sort t.processInstance limit 1 " +
                " update t with { \"sentEvents\": PUSH(t.sentEvents, @taskEvent) } in task_instances RETURN NEW";

        final Map<String,Object> bindVariables = new MapBuilder()
                .put("currentTaskStatus", currentTaskStatus)
                .put("currentProcessStatus", currentProcessStatus)
                .put("taskDependsOnStatus",taskDependsOnStatus)
                .put("taskEvent",taskInstanceEvent)
                .get();

        ArangoCursor<TaskInstance> taskInstances = arangoOperations.query(query,bindVariables,null,TaskInstance.class);
        return
                (taskInstances.hasNext())?Optional.of(taskInstances.next()):Optional.empty();
    }


    public Optional<TaskInstance> findByStatus(TaskInstanceStatus status
                        ,TaskInstanceEvent taskInstanceEvent){
        final String query = "for i in task_instances \n" +
                " filter i.status == @status " +
                " filter i.isTemplate == false " +
                " filter @taskEvent not in i.sentEvents OR i.sentEvents == NULL " +
                " sort i.processInstance limit 1 " +
                " update i with { \"sentEvents\": PUSH(i.sentEvents, @taskEvent) } in task_instances  RETURN NEW \n";

        final Map<String,Object> bindVariables = new MapBuilder()
                .put("status", status)
                .put("taskEvent",taskInstanceEvent)
                .get();

        ArangoCursor<TaskInstance> taskInstances = arangoOperations.query(query,bindVariables,null,TaskInstance.class);
        return
                (taskInstances.hasNext())?Optional.of(taskInstances.next()):Optional.empty();

    };

}
