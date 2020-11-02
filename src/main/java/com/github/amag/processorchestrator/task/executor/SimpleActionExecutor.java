package com.github.amag.processorchestrator.task.executor;

import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class SimpleActionExecutor {

    private final TaskInstanceRepository taskInstanceRepository;

    public void execute(SimpleAction simpleAction, UUID taskInstanceId){
        Optional<TaskInstance> optionalTaskInstance = taskInstanceRepository.findById(taskInstanceId);

        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
            Map<String, Object> input = new HashMap<>();

            if(taskInstance.getDependsOn() != null && taskInstance.getDependsOn().size()>0 ){
                taskInstance.getDependsOn().forEach(x->
                {
                    if(x.getOutput()!=null)
                        input.putAll(x.getOutput());
                });
            }
            Map<String, Object> output = simpleAction.execute(taskInstance.getProcessInstance().getProcessContext(), input);
                if (output != null) {
                    taskInstance.setOutput(output);
                }
           taskInstanceRepository.save(taskInstance);
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

}
