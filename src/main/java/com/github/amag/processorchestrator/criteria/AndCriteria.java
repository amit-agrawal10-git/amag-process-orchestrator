package com.github.amag.processorchestrator.criteria;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.BaseObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AndCriteria<T extends BaseObject> implements Criteria<T> {

    private final Criteria<T> firstCriteria, secondCriteria;

    @Override
    public T evaluate(T o, ArangoOperations arangoOperations) {
        T x = firstCriteria.evaluate(o, arangoOperations);
        return (!x.isCriteriaResult())?x:secondCriteria.evaluate(x, arangoOperations);
    }
}
