package com.github.amag.processorchestrator.context;

import com.github.amag.processorchestrator.domain.ProcessInstance;

import java.util.HashMap;
import java.util.Map;

public class ProcessContext {

    private Map<String,Object> map = new HashMap();

    public Object getContextValue(String key) {
        return map.get(key);
    }

    public void setContextValue(String key,Object value) {
        map.put(key,value);
    }


}
