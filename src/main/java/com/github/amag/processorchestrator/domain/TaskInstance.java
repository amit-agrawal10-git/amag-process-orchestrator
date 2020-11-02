package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(value = "task_instances", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstance extends BaseObject {

    private TaskInstanceStatus status;

    @CreatedDate
    private Date createdWhen;

    @LastModifiedDate
    private Date modifiedWhen;

    private boolean isTemplate;

    @Ref
    private TaskTemplate taskTemplate;

    @Ref
    private ProcessTemplate processTemplate;

    @Ref
    private List<TaskInstance> dependsOn;

    @Ref
    private ProcessInstance processInstance;

    private Map<String, Object> output;

}

