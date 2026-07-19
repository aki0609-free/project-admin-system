package com.project.backend.app.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.app.job.entity.AsyncJob;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {

    List<AsyncJob> findTop10ByStatusOrderByCreatedAt(String status);
    
}
