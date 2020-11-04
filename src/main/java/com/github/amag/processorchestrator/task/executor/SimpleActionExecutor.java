package com.github.amag.processorchestrator.task.executor;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.repositories.TaskInstanceRepository;
import com.github.amag.processorchestrator.task.types.SimpleAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class SimpleActionExecutor {

    private final ArangoOperations arangoOperations;

    public void execute(SimpleAction simpleAction, UUID taskInstanceId) {
        Optional<TaskInstance> optionalTaskInstance = arangoOperations.find(taskInstanceId, TaskInstance.class);

        optionalTaskInstance.ifPresentOrElse(taskInstance -> {
          Object output =  simpleAction.execute(UUID.fromString(taskInstance.getArangoKey()),arangoOperations);
          taskInstance.getBaseAction().setOutput(output);
          arangoOperations.repsert(taskInstance);
        }, () -> {
            log.debug("Expected task instance not found");
        });
    }

}
