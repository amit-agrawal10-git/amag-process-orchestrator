package com.github.amag.processorchestrator.scheduler;

import com.github.amag.processorchestrator.services.ProcessManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessScheduler {

    private final ProcessManager processManager;
    @Value("${amag.process.job.start.limit:4}")
    private int maximumActiveProcess;

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
        processManager.startProcess(maximumActiveProcess);
    }

    @Scheduled(fixedRateString = "${amag.process.job.finish.delay:3000}")
    protected void finishJob() {
        processManager.completeProcess();
    }

}
