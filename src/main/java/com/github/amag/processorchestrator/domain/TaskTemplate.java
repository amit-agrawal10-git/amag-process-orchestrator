package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Relations;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateDependsOnTaskTemplate;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateIsPartOfProcessTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Document(value = "task-templates", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplate extends BaseObject {

    private String name, instanceClass, instanceMethod;

    @Relations(edges = TaskTemplateDependsOnTaskTemplate.class,lazy = true)
    private List<TaskTemplate> dependsOn;

    @Relations(edges = TaskTemplateIsPartOfProcessTemplate.class,lazy = true)
    private ProcessTemplate processTemplate;

}

