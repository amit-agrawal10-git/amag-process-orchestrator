package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.*;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessServiceImpl implements ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;


    @Scheduled(fixedDelayString = "5000") // run with 5 sec delay
    @Transactional
    protected void executeJob(){
        log.debug("Running async job");
        for (Process process:processRepository.findAllByExecutedUptoBefore(new Date())
             ) {
            Date startDate = (process.getExecutedUpto()!=null)?process.getExecutedUpto():process.getFrom();
            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            start.setTime(startDate);
            start.add(Calendar.DATE, 1);

            Calendar end = Calendar.getInstance();
            end.setTime(new Date());

            for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1),
                    date = start.getTime()) {
                TaskInstance processTaskInstance = TaskInstance
                        .builder()
                        .status("DONE")
                        .build();

                ProcessInstance processInstance = ProcessInstance.builder()
                        .process(process)
                        .status("PENDING")
                        .name("TEST")
                        .build();
                List<TaskInstance> processTaskInstances = new ArrayList<>();
                processTaskInstances.add(processTaskInstance);
                taskInstanceRepository.save(processTaskInstance);
                processInstanceRepository.save(processInstance);
                // Create Process Instance
                // Instantiate Object of Process Instance class
                // Update Executed up to

            }
            }


        }


}
