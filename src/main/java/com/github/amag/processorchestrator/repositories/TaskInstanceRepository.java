package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.ProcessTaskTemplate;
import com.github.amag.processorchestrator.domain.TaskInstance;

import java.util.UUID;

public interface TaskInstanceRepository extends ArangoRepository<TaskInstance, UUID> {
}
