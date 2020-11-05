package com.github.amag.processorchestrator.criteria;

import com.github.amag.processorchestrator.domain.BaseObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrCriteria<T extends BaseObject> implements Criteria<T> {

    private final Criteria<T> firstCriteria, secondCriteria;

    @Override
    public T evaluate(T o) {
        T x = firstCriteria.evaluate(o);
        return (x.isCriteriaResult())?x:secondCriteria.evaluate(o);
    }
}
