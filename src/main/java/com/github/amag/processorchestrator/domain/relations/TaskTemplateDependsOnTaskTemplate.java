package com.github.amag.processorchestrator.domain.relations;

import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import com.github.amag.processorchestrator.domain.TaskTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Edge(allowUserKeys = true, keyType = KeyType.uuid, value = "task-template-depends-on")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplateDependsOnTaskTemplate extends BaseRelation {

    @From
    private TaskTemplate fromTaskTemplate;

    @To
    private TaskTemplate toTaskTemplate;

}
