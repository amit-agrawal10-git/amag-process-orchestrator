package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.task.types.BaseAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Document(value = "task_instances", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstance extends BaseObject {

    private String name, description;

    private BaseAction baseAction;

    private TaskInstanceStatus status;

    private boolean isTemplate;

    @Ref(lazy = true)
    private ProcessInstance processTemplate;

    @Ref(lazy = false)
    private List<TaskInstance> dependsOn;

    @Ref(lazy = true)
    private ProcessInstance processInstance;

    @Ref(lazy = true)
    private TaskInstance taskTemplate;

    private Object output;

    private Set<TaskInstanceEvent> sentEvents;

}

