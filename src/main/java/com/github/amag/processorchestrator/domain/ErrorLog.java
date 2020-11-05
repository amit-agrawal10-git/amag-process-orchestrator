package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.Ref;
import com.github.amag.processorchestrator.domain.enums.ErrorLogTypes;
import com.github.amag.processorchestrator.domain.enums.ProcessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.util.Date;
import java.util.UUID;

@Document(value = "process_error_log", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog extends BaseObject {

    @CreatedDate
    private Date createdWhen;

    private ErrorLogTypes entityType;

    private UUID entityId;

    private String stackTrace;
}


