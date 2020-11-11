package com.github.amag.processorchestrator.scheduler;

import com.github.amag.processorchestrator.services.TaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
@Profile("!micro")
public class ProcessTaskScheduler {

    private final TaskManager taskManager;
    @Value("${amag.task.job.start.limit:10}")
    private int maximumActiveTask;

    @Scheduled(fixedDelayString = "${amag.task.job.start.delay}")
    @Transactional
    protected void startTask() {
        taskManager.startMultipleTask(maximumActiveTask);
    }

    @Scheduled(fixedDelayString = "${amag.task.job.ready.delay}")
    @Transactional
    protected void findReadyTask() {
        taskManager.findAndMarkReadyTask();
    }

}
