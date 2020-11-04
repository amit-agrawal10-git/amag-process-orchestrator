package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.context.ProcessContext;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

@Document(value = "process_instances", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@HashIndex(fields = {"processDate","process"}, unique = true)
public class ProcessInstance extends BaseObject {

    private ProcessInstanceStatus status;

    @CreatedDate
    private Date createdWhen;

    @LastModifiedDate
    private Date modifiedWhen;

    private Date processDate;

    @Ref(lazy = true)
    private Process process;

    private ProcessContext processContext;

}

