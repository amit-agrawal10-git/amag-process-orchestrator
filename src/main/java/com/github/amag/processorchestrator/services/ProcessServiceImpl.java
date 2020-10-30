package com.github.amag.processorchestrator.services;

import com.github.amag.processorchestrator.domain.*;
import com.github.amag.processorchestrator.domain.Process;
import com.github.amag.processorchestrator.repositories.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProcessServiceImpl implements ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;


    @Scheduled(fixedDelayString = "10000") // run with 5 sec delay
    @Transactional
    protected void executeJob() {
        log.debug("Running async job");
        for (final Process process : processRepository.findAllByExecutedUptoBefore(new Date())
        ) {
            Date startDate = (process.getExecutedUpto() != null) ? process.getExecutedUpto() : process.getFrom();
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
                ProcessInstance processInstance = ProcessInstance.builder()
                        .process(process)
                        .status("PENDING")
                        .name("TEST")
                        .build();

                List<TaskInstance> taskInstances = taskInstanceRepository.findLastTaskInstancesByProcessTemplateArangoId(process.getProcessTemplate().getArangoId());
                List<TaskInstance> newTaskInstances = new ArrayList<TaskInstance>();
                List<TaskInstance2TaskInstance> tempInstances = new ArrayList<>();
                copy(processInstance,null, taskInstances, newTaskInstances, tempInstances);
                tempInstances.forEach(x-> {
                    x.from.getDependsOn().add(x.to);
                });
                processInstanceRepository.save(processInstance);
                for (int i=newTaskInstances.size()-1;i>=0;i--)
                    taskInstanceRepository.save(newTaskInstances.get(i));
                log.debug("done");
                process.setExecutedUpto(date);
                processRepository.save(process);
            }
        }
    }

    private void copy(ProcessInstance processInstance,TaskInstance dependentInstance, List<TaskInstance> taskInstances, List<TaskInstance> resultTaskInstances, List<TaskInstance2TaskInstance> instance2TaskInstances) {
        if (taskInstances != null) {
            taskInstances.stream().forEach(x ->
            {
                x.setArangoId(null);
                x.setArangoKey(null);
                x.setTemplate(false);
                x.setCreatedWhen(null);
                x.setModifiedWhen(null);
                x.setProcessInstance(processInstance);
                if (resultTaskInstances.contains(x)) {
                    TaskInstance y = resultTaskInstances.get(resultTaskInstances.indexOf(x));
                    if (dependentInstance != null)
                        insertToTempList(dependentInstance, y, instance2TaskInstances);
                } else {
                    resultTaskInstances.add(x);
                }
            });
            taskInstances.stream().forEach(x ->
            {
                if (x.getDependsOn() != null)
                    copy(processInstance, x, x.getDependsOn(), resultTaskInstances, instance2TaskInstances);
            });
        }
    }

    private void insertToTempList(TaskInstance from, TaskInstance to, List<TaskInstance2TaskInstance> instance2TaskInstances){
        TaskInstance2TaskInstance taskInstance2TaskInstance = new TaskInstance2TaskInstance();
        taskInstance2TaskInstance.from=from;
        taskInstance2TaskInstance.to=to;
        instance2TaskInstances.add(taskInstance2TaskInstance);
    }


    private class TaskInstance2TaskInstance
    {
        public TaskInstance from, to;

    }

}
