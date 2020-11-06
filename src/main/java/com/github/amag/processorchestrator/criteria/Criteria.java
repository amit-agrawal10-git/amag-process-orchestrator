package com.github.amag.processorchestrator.criteria;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.BaseObject;

public interface Criteria<T extends BaseObject> {

    public T evaluate(T o, ArangoOperations arangoOperations);

}
