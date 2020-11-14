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
public class ProcessScheduler {

    private final ProcessManager processManager;

    @Scheduled(fixedDelayString = "${amag.process.job.instantiate.delay:5000}")
    @Transactional
    protected void instantiateJob() {
        processManager.instantiateJob();
    }

    @Scheduled(fixedRateString = "${amag.process.job.ready.delay:1000}")
    protected void markreadyJob() {
        processManager.findAndMarkReady();
    }

    @Scheduled(fixedRateString = "${amag.process.job.start.delay:1000}")
    protected void startJob() {
        processManager.startProcess();
    }

    @Scheduled(fixedRateString = "${amag.process.job.finish.delay:3000}")
    protected void finishJob() {
        processManager.completeProcess();
    }

}
