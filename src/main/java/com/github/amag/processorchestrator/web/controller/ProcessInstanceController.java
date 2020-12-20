package com.github.amag.processorchestrator.web.controller;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.repositories.ProcessInstanceRepository;
import com.github.amag.processorchestrator.services.ProcessManager;
import com.github.amag.processorchestrator.smconfig.events.ProcessEventManager;
import com.github.amag.processorchestrator.web.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
@Slf4j
public class ProcessInstanceController {

    private final ProcessManager processManager;
    private final ProcessEventManager processEventManager;
    private final ArangoOperations arangoOperations;
    private final ProcessInstanceRepository processInstanceRepository;

    @GetMapping(path = "/processinstances")
    public String listInstances(
            Model model,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "templateId", required = false) String templateId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page,size);
        Page<ProcessInstance> processInstancePage = null;
        if(status==null && templateId==null)
            processInstancePage = processInstanceRepository.findAllByIsTemplateFalse(pageable);
        if(status!=null && templateId==null)
            processInstancePage = processInstanceRepository.findAllByStatusAndIsTemplateFalse(ProcessInstanceStatus.valueOf(status),pageable);
        if(templateId!=null)
        {
            ProcessInstance processTemplate = arangoOperations.find(UUID.fromString(templateId), ProcessInstance.class).get();
            log.debug("processTemplate {}",processTemplate);
            if(status!=null)
                processInstancePage = processInstanceRepository.findAllByStatusAndProcessTemplate(status,processTemplate.getArangoId(),pageable);
            else
                processInstancePage = processInstanceRepository.findAllByProcessTemplate(processTemplate.getArangoId(),pageable);
        }

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

    @GetMapping(path = "/processinstance/stat/{templateKey}")
    public String listInstanceStat(Model model, @PathVariable(value = "templateKey") String templateKey) {

        log.debug("templateKey {}",templateKey);
        final String query = " for r in @@instcollection " +
                " filter r._key == @templateKey " +
                " for d in @@instcollection " +
                " filter d.processTemplate == r._id"+
                " collect s = d.status, t = r._key" +
                " aggregate c = count(d._id)" +
                " return { \"status\":s, \"templateId\":t, \"count\":c}";
        log.debug("query {}",query);

        Map<String,Object> bindVar = new MapBuilder()
                .put("@instcollection",ProcessInstance.class)
                .put("templateKey",templateKey)
                .get();

        ArangoCursor<Map> arangoCursor = arangoOperations.query(query,bindVar,null,Map.class);
        model.addAttribute("pistat",arangoCursor.asListRemaining());

        return "listInstanceStat";
    }

    @GetMapping(path = "/processinstance/stat")
    public String listInstanceStatGeneral(Model model) {

        final String query = " for r in @@instcollection " +
                " filter r.isTemplate == false \n" +
                "collect s = r.status aggregate c = count(r._id) " +
                " return { \"status\":s, \"templateId\":null, \"count\":c}" +
                "";
        log.debug("query {}",query);

        Map<String,Object> bindVar = new MapBuilder()
                .put("@instcollection",ProcessInstance.class)
                .get();

        ArangoCursor<Map> arangoCursor = arangoOperations.query(query,bindVar,null,Map.class);
        model.addAttribute("pistat",arangoCursor.asListRemaining());

        return "listInstanceStat";
    }

    @GetMapping(path = "/processinstance/rollback/{id}")
    public String rollback(Model model, @PathVariable("id") UUID id){
        Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(id,ProcessInstance.class);
        optionalProcessInstance.ifPresent(taskInstance -> {
            processEventManager.sendProcessInstanceEvent(id, ProcessInstanceEvent.ROLLED_BACK);
        });

        return "redirect:/api/v1/processinstances";
    }

    @DeleteMapping(path = "/processinstance/{id}")
    @ResponseBody
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id){
        ProcessInstance processInstance = arangoOperations.find(id,ProcessInstance.class).orElseThrow(NotFoundException::new);
        processManager.deleteProcessInstance(processInstance);
    }


}
