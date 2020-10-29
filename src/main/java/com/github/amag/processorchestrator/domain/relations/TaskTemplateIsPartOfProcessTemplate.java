package com.github.amag.processorchestrator.domain.relations;

import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import com.github.amag.processorchestrator.domain.ProcessTemplate;
import com.github.amag.processorchestrator.domain.TaskTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Edge(allowUserKeys = true, keyType = KeyType.uuid, value = "task-template-part-of-process-template")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplateIsPartOfProcessTemplate extends BaseRelation {


    @From
    private TaskTemplate taskTemplate;

    @To
    private ProcessTemplate processTemplate;

}
