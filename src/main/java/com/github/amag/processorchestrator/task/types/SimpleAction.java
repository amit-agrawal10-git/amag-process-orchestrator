package com.github.amag.processorchestrator.task.types;

import com.github.amag.processorchestrator.context.ProcessContext;

import java.util.Map;

public interface SimpleAction extends BaseAction {

    public Map<String, Object> execute(ProcessContext processContext, Map<String, Object> input);

}
