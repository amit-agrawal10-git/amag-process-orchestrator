package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.platform.domain.BaseObject;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(value = "processes", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@HashIndex(fields = {"processCode"}, unique = true)
public class Process extends BaseObject {

    private String instantiationActionBean;

    private ProcessStatus processStatus;

    private String processCode;

}


