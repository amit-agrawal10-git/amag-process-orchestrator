package com.github.amag.processorchestrator.web.controller;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Controller
public class TaskInstanceController {

    private final TaskInstanceRepository taskInstanceRepository;

    @GetMapping(path = "/taskinstances")
    public String listInstances(
            Model model,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page,size);
        Page<TaskInstance> taskInstancePage = null;

        if(processInstanceId == null)
            taskInstancePage = taskInstanceRepository.findAll(pageable);
        else
        {
            taskInstancePage = taskInstanceRepository.findAllByProcessInstance(processInstanceId,pageable);
        }

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

}
