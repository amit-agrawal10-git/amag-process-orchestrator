package com.github.amag.processorchestrator.web.controller;

import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.repositories.ProcessRepository;
import com.github.amag.processorchestrator.web.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class ProcessController {

    private final ProcessRepository repository;

    @GetMapping(produces = { "application/json" }, path = "processes")
    @ResponseStatus(HttpStatus.OK)
    public Page<Process> list(Pageable pageable){
        return repository.findAll(pageable);
    }

    @GetMapping("process/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Process getProcessById(@Validated @PathVariable("id") UUID id){
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    @PostMapping(path = "process")
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody @Validated Process process){
        repository.save(process);
    }

    @PutMapping("process/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable("id") UUID id, @RequestBody @Validated Process process){

        final Process foundProcess = repository.findById(id).orElseThrow(NotFoundException::new);
        foundProcess.setProcessStatus(process.getProcessStatus());
        foundProcess.setExecutedUpto(process.getExecutedUpto());
        foundProcess.setFrom(process.getFrom());
        foundProcess.setProcessTemplate(process.getProcessTemplate());
        repository.save(foundProcess);
    }

    @DeleteMapping("process/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id){
        repository.deleteById(id);
    }


}
