package com.github.amag.processorchestrator.domain.relations;

import com.arangodb.springframework.annotation.ArangoId;

public abstract class BaseRelation {
    @ArangoId
    protected String key;
}
