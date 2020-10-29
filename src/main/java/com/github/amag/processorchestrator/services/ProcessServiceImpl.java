package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.ProcessTemplate;
import com.github.amag.processorchestrator.domain.TaskTemplate;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateDependsOnTaskTemplate;
import com.github.amag.processorchestrator.domain.relations.TaskTemplateIsPartOfProcessTemplate;
import com.github.amag.processorchestrator.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProcessServiceImpl implements ProcessService {

    private final ProcessTemplateRepository processTemplateRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskTemplateDependsOnRepository taskTemplateDependsOnRepository;
    private final TaskTemplateIsPartOfRepository taskTemplateIsPartOfRepository;
    private final ProcessStatusRepository processStatusRepository;
    private static Boolean isRunning;

    @Override
    public void saveTaskRelations(ProcessTemplate processTemplate, TaskTemplate taskTemplate, List<TaskTemplate> dependsOn) {

        if (taskTemplate.getProcessTemplate() == null) {
            TaskTemplateIsPartOfProcessTemplate taskTemplateIsPartOfProcessTemplate = TaskTemplateIsPartOfProcessTemplate.builder()
                    .processTemplate(processTemplate)
                    .taskTemplate(taskTemplate)
                    .build();
            taskTemplateIsPartOfRepository.save(taskTemplateIsPartOfProcessTemplate);
        }
        if (dependsOn != null && !dependsOn.isEmpty()){
            for (TaskTemplate dependsTT:dependsOn) {
                if(taskTemplate.getDependsOn()==null || !taskTemplate.getDependsOn().contains(dependsTT)){
                    TaskTemplateDependsOnTaskTemplate taskTemplateDependsOnTaskTemplate = TaskTemplateDependsOnTaskTemplate.builder()
                    .fromTaskTemplate(taskTemplate)
                    .toTaskTemplate(dependsTT)
                    .build();
                    taskTemplateDependsOnRepository.save(taskTemplateDependsOnTaskTemplate);
                }
            }
        }
    }


}
