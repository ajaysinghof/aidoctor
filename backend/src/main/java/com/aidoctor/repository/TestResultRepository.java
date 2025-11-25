package com.aidoctor.repository;
import com.aidoctor.model.TestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestResultRepository extends JpaRepository<TestResultEntity, Long> { }
