package com.github.amag.processorchestrator.smconfig.actions;

import com.arangodb.springframework.core.ArangoOperations;
import com.github.amag.processorchestrator.context.ProcessContext;
import com.github.amag.processorchestrator.domain.ProcessInstance;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceEvent;
import com.github.amag.processorchestrator.domain.enums.ProcessInstanceStatus;
import com.github.amag.processorchestrator.smconfig.ProcessInstanceStateMachineConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartProcessAction implements Action<ProcessInstanceStatus, ProcessInstanceEvent> {

    private final ArangoOperations arangoOperations;

    @Override
    public void execute(StateContext<ProcessInstanceStatus, ProcessInstanceEvent> stateContext) {
        log.debug("start process instance was called");
        UUID instanceId = UUID.fromString(stateContext.getMessageHeader(ProcessInstanceStateMachineConfig.PROCESS_INSTANCE_ID_HEADER).toString());
        Optional<ProcessInstance> optionalProcessInstance = arangoOperations.find(instanceId,ProcessInstance.class);

        optionalProcessInstance.ifPresentOrElse(instance -> {

              ProcessContext processContext = new ProcessContext();
                instance.setProcessContext(processContext);
                arangoOperations.repsert(instance);

            }, () ->
                        log.error("Process Instance Not Found Id: {}", instanceId)
        );
    }
}
