package com.github.amag.processorchestrator.repositories;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class ProcessInstanceRepository {

    private final ArangoOperations arangoOperations;

    public Optional<ProcessInstance> findByStatusAndIsTemplate(ProcessInstanceStatus processInstanceStatus, ProcessInstanceEvent processInstanceEvent)
    {
        final String query = "for i in process_instances \n" +
                " filter i.status == @status " +
                " FILTER @processEvent not in i.sentEvents OR i.sentEvents == NULL \n" +
                " filter i.isTemplate == false " +
                " sort rand() limit 1 " +
                " update i with { \"sentEvents\": PUSH(i.sentEvents, @processEvent) } in process_instances RETURN NEW";
        final Map<String,Object> bindVariables = new MapBuilder()
                .put("status", processInstanceStatus)
                .put("processEvent",processInstanceEvent)
                .get();

        ArangoCursor<ProcessInstance> processInstances = arangoOperations.query(query,bindVariables,null,ProcessInstance.class);
        return
                (processInstances.hasNext())?Optional.of(processInstances.next()):Optional.empty();
    };

    public Optional<ProcessInstance> findCompletedProcessInstance(@Param("currentTaskStatus") TaskInstanceStatus currentTaskStatus,
                                               @Param("currentProcessStatus") ProcessInstanceStatus currentProcessStatus,
                                                                 ProcessInstanceEvent processInstanceEvent){
        final String query = "for p in process_instances \n" +
                "   filter p.isTemplate == false \n" +
                "   filter p.status == @currentProcessStatus\n" +
                " FILTER @processEvent not in p.sentEvents OR p.sentEvents == NULL \n" +
                "   filter (\n" +
                "   for t in task_instances \n" +
                "   filter t.isTemplate == false \n" +
                "   filter t.processInstance == p._id\n" +
                "   return t.status) ALL IN [@currentTaskStatus]\n" +
                "   limit 1 " +
                " update p with { \"sentEvents\": PUSH(p.sentEvents, @processEvent) } in process_instances RETURN NEW";
        final Map<String,Object> bindVariables = new MapBuilder()
                .put("currentTaskStatus", currentTaskStatus)
                .put("currentProcessStatus",currentProcessStatus)
                .put("processEvent",processInstanceEvent)
                .get();

        ArangoCursor<ProcessInstance> processInstances = arangoOperations.query(query,bindVariables,null,ProcessInstance.class);
        return
                (processInstances.hasNext())?Optional.of(processInstances.next()):Optional.empty();
    }


    public long countByStatus(ProcessInstanceStatus processInstanceStatus){
        final String query = " for r in process_instances " +
                " filter r.status == @status " +
                " return r ";

        final Map<String,Object> bindVariables = new MapBuilder()
                .put("status", processInstanceStatus)
                .get();

        ArangoCursor<ProcessInstance> processInstances = arangoOperations.query(query,bindVariables,new AqlQueryOptions().count(true),ProcessInstance.class);
        return processInstances.count();
    }

}
