package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Document(value = "process_task_templates", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessTaskTemplate extends BaseObject {


    @Ref
    private TaskTemplate taskTemplate;

    @Ref
    private List<ProcessTaskTemplate> dependsOn;

    @Ref
    private ProcessTemplate processTemplate;

}

