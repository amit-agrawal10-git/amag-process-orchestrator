package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.platform.criteria.Criteria;
import com.github.amag.platform.domain.BaseObject;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Document(value = "process_instances", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProcessInstance extends BaseObject {

    private String name;

    @Ref(lazy = true)
    private Process process;

    private ProcessInstanceStatus status;

    private Date processDate;

    @Ref(lazy = true)
    private ProcessInstance processTemplate;

    private boolean isTemplate;

    private Criteria<ProcessInstance> instantiationCriteria;

    private Criteria<ProcessInstance> executionCriteria;

    private Set<ProcessInstanceEvent> sentEvents;

}

