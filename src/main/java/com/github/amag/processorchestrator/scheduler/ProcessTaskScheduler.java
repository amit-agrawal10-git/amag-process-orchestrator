package com.github.amag.processorchestrator.scheduler;

import com.github.amag.processorchestrator.services.TaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessTaskScheduler {

    private final TaskManager taskManager;

    @Scheduled(initialDelayString = "${amag.task.job.start.initial.delay:70000}",fixedRateString = "${amag.task.job.start.delay:500}")
    protected void startTask() {
        taskManager.startTask();
    }

    @Scheduled(initialDelayString = "${amag.task.job.ready.initial.delay:75000}",fixedRateString = "${amag.task.job.ready.delay:500}")
    protected void findReadyTask() {
        taskManager.findAndMarkReadyTask();
    }

    @Scheduled(initialDelayString = "${amag.task.job.execute.initial.delay:80000}",fixedRateString = "${amag.task.job.execute.delay:500}")
    protected void executeTask() {
        taskManager.executeTask();
    }

}
