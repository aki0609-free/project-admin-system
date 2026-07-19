package com.project.backend.app.job.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.project.backend.app.job.entity.AsyncJob;
import com.project.backend.app.job.repository.AsyncJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncJobService {

    private final AsyncJobRepository jobRepository;

    public void enqueue(String type, String payload) {

        AsyncJob job = new AsyncJob();

        job.setType(type);
        job.setPayload(payload);
        job.setStatus("PENDING");
        job.setCreatedAt(Instant.now());

        jobRepository.save(job);
    }
    
}
