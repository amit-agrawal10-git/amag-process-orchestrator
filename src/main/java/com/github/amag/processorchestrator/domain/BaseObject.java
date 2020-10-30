package com.github.amag.processorchestrator.domain;

import com.arangodb.springframework.annotation.ArangoId;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public abstract class BaseObject {

    @Id
    protected String arangoKey;

    @ArangoId
    protected String arangoId;
}
