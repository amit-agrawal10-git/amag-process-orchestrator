package com.github.amag.processorchestrator.web.controller;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceEvent;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.smconfig.events.ProcessEventManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Controller
public class ProcessInstanceController {

    private final ProcessInstanceRepository processInstanceRepository;
    private final ProcessEventManager processEventManager;
    private final ArangoOperations arangoOperations;

    @GetMapping(path = "/processinstances")
    public String listInstances(
            Model model,
            @RequestParam(value = "status", defaultValue = "FAILED") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page,size);
        Page<ProcessInstance> processInstancePage = processInstanceRepository.findAllByStatusAndIsTemplateFalse(ProcessInstanceStatus.valueOf(status),pageable);

        model.addAttribute("processInstancePage", processInstancePage);

        int totalInstances = processInstancePage.getTotalPages();
        if (totalInstances > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalInstances)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "listProcessInstances";
    }

    @GetMapping(path = "/instance/stat")
    public String listInstanceStat(Model model) {

        final String query = " for r in @@instcollection " +
                " filter r.isTemplate == false " +
                " collect s = r.status " +
                " aggregate c = count(r._id)" +
                " return { \"status\":s, \"count\":c}";

        Map<String,Object> bindVar = new MapBuilder()
                .put("@instcollection",ProcessInstance.class)
                .get();

        ArangoCursor<Map> arangoCursor = arangoOperations.query(query,bindVar,null,Map.class);
        model.addAttribute("pistat",arangoCursor.asListRemaining());

        bindVar = new MapBuilder()
                .put("@instcollection",TaskInstance.class)
                .get();

        arangoCursor = arangoOperations.query(query,bindVar,null,Map.class);
        model.addAttribute("tistat",arangoCursor.asListRemaining());

        return "listInstanceStat";
    }

    @GetMapping(path = "/processinstance/rollback/{id}")
    public String rollback(Model model, @PathVariable("id") UUID id){
        Optional<ProcessInstance> optionalProcessInstance = processInstanceRepository.findById(id);
        optionalProcessInstance.ifPresent(taskInstance -> {
            processEventManager.sendProcessInstanceEvent(id, ProcessInstanceEvent.ROLLED_BACK);
        });

        return "redirect:/api/v1/processinstances";
    }

}
