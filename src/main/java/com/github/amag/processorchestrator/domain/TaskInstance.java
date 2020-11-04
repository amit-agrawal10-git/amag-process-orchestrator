package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import com.github.amag.processorchestrator.task.types.BaseAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.List;

@Document(value = "task_instances", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstance extends BaseObject {

    private String name, description;
    private BaseAction baseAction;

    private TaskInstanceStatus status;

    @CreatedDate
    private Date createdWhen;

    @LastModifiedDate
    private Date modifiedWhen;

    private boolean isTemplate;

    @Ref(lazy = true)
    private ProcessTemplate processTemplate;

    @Ref(lazy = false) // todo to change
    private List<TaskInstance> dependsOn;

    @Ref(lazy = true)
    private ProcessInstance processInstance;

    // todo improve data storage of tast instanc, avoid duplicate data
    @Ref(lazy = true)
    private TaskInstance taskTemplate;

    private Object output;
}

