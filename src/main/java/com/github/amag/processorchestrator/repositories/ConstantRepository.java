package com.github.amag.processorchestrator.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.github.amag.processorchestrator.domain.Constant;
import com.github.amag.processorchestrator.domain.TaskInstance;
import com.github.amag.processorchestrator.domain.enums.TaskInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ConstantRepository extends ArangoRepository<Constant, UUID> {
    Optional<Constant> findByKey(String key);
}
