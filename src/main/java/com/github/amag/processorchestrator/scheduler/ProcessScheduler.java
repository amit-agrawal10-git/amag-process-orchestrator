package com.github.amag.processorchestrator.scheduler;

import com.github.amag.processorchestrator.services.ProcessManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
@Profile("!micro")
public class ProcessScheduler {

    private final ProcessManager processManager;

    @Scheduled(fixedDelayString = "${amag.process.job.instantiate.delay}")
    @Transactional
    protected void instantiateJob() {
        processManager.instantiateJob();
    }

    @Scheduled(fixedDelayString = "${amag.process.job.start.delay}")
    @Transactional
    protected void startJob() {
        processManager.startMultipleProcess();
    }

    @Scheduled(fixedDelayString = "${amag.process.job.finish.delay}")
    @Transactional
    protected void finishJob() {
        processManager.completeProcess();
    }

}
