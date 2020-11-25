package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.criteria.Criteria;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import com.github.amag.processorchestrator.process.actions.BaseProcessAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Document(value = "processes", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@HashIndex(fields = {"processCode"}, unique = true)
public class Process extends BaseObject {

    @Ref(lazy = true)
    private ProcessInstance processTemplate;

    private String instantiationActionBean;

    private ProcessStatus processStatus;

    private String processCode;

}


