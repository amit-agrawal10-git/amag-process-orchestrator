package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.ProcessTemplate;
import com.github.amag.processorchestrator.domain.TaskTemplate;

import java.util.List;

public interface ProcessService {
    public void saveTaskRelations(ProcessTemplate processTemplate, TaskTemplate taskTemplate, List<TaskTemplate> dependsOn);
}
