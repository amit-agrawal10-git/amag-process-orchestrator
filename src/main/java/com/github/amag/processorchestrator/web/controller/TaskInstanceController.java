package com.github.amag.processorchestrator.web.controller;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.services.TaskManager;
import com.github.amag.processorchestrator.smconfig.events.TaskEventManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Controller
public class TaskInstanceController {

    private final TaskManager taskManager;
    private final TaskEventManager taskEventManager;
    private final ArangoOperations arangoOperations;

    @GetMapping(path = "/taskinstances")
    public String listInstances(
            Model model,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String processTemplateId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page,size);
        Page<TaskInstance> taskInstancePage = null;

        if(processInstanceId == null && processTemplateId == null)
            taskInstancePage = taskManager.findAllTaskInstances(pageable);
        else if (processInstanceId != null)
        {
            taskInstancePage = taskManager.findAllTaskInstancesByProcessInstance(processInstanceId,pageable);
        } else
            taskInstancePage = taskManager.findAllTaskInstancesByProcessTemplate(processTemplateId,pageable);

        model.addAttribute("taskInstancePage", taskInstancePage);

        int totalInstances = taskInstancePage.getTotalPages();
        if (totalInstances > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalInstances)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "listTaskInstances";
    }

    @GetMapping(path = "/taskinstance/rollback/{id}")
    public String rollback(Model model, @PathVariable("id") UUID id){
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(id,TaskInstance.class);
        optionalTaskInstance.ifPresent(taskInstance -> {
            taskEventManager.sendTaskInstanceEvent(id, TaskInstanceEvent.ROLLED_BACK);
        });

        if(optionalTaskInstance.isPresent())
            return "redirect:/api/v1/taskinstances?processInstanceId="+optionalTaskInstance.get().getProcessInstance().getArangoId();
        return "redirect:/api/v1/taskinstances";
    }

}
