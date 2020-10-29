package com.github.amag.processorchestrator.domain;

import com.arangodb.springframework.annotation.ArangoId;
import lombok.Data;

@Data
public abstract class BaseObject {
    @ArangoId
    protected String key;
}
