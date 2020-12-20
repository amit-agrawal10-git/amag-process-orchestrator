package com.github.amag.processorchestrator.process.actions;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.services.TaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BaseProcessAction {
    public abstract ProcessInstance instantiate(Process process);

    protected List<TaskInstance> createTaskInstancesFromTemplate(ProcessInstance processTemplate, TaskManager taskManager){
        List<TaskInstance> taskInstances = taskManager.findAllTaskTemplatesByProcessTemplateId(processTemplate.getArangoId());

        if(taskInstances!=null && !taskInstances.isEmpty())
        taskInstances.forEach( x-> {
            if(x.getDependsOn() != null){
                List<TaskInstance> tempList = new ArrayList<>();
                x.getDependsOn().forEach(y-> {
                    tempList.add(getTaskInstanceById(taskInstances,y.getArangoId()));
                });
                x.setDependsOn(tempList);
            }
        });
        else
            return new ArrayList<>();

        return taskInstances;
    }

    protected TaskInstance getTaskInstanceById(List<TaskInstance> taskInstances, String taskInstanceId){
        for (TaskInstance taskInstance:taskInstances) {
            if(taskInstance.getArangoId().equals(taskInstanceId))
                return taskInstance;
        }
        return null;
    }

    protected void removeUnwantedParams(ProcessInstance processInstance, List<TaskInstance> taskInstances, ArangoOperations arangoOperations){
        taskInstances.stream().forEach(x ->
        {
            x.setTaskTemplate(arangoOperations.find(UUID.fromString(x.getArangoKey()), TaskInstance.class).get());
            x.setArangoId(null);
            x.setArangoKey(null);
            x.setTemplate(false);
            x.setCreatedWhen(null);
            x.setModifiedWhen(null);
            x.setBaseTaskAction(null);
            x.setStatus(TaskInstanceStatus.PENDING);
            x.setDescription(null);
            x.setProcessTemplate(null);
            x.setProcessInstance(processInstance);
        });
    }
}
