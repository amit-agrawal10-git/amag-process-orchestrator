package com.github.amag.processorchestrator.initiator;

import com.github.amag.processorchestrator.context.ProcessContext;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public abstract class Processor implements Executable {

    @Autowired
    private ProcessContext processContext;
    
}
