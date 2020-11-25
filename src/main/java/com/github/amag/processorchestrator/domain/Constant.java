package com.github.amag.processorchestrator.domain;


import com.arangodb.entity.KeyType;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.github.amag.processorchestrator.domain.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Document(value = "constants", keyType = KeyType.uuid, allowUserKeys = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@HashIndex(fields = {"key"},unique = true)
public class Constant extends BaseObject {

    private String key;
    private Date dateValue;
    private Boolean booleanValue;
    private Double doubleValue;
    private String stringValue;

}


