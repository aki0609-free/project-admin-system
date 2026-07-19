package com.project.backend.app.job.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.app.job.entity.AsyncJob;
import com.project.backend.app.job.repository.AsyncJobRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncJobWorker {

    private final AsyncJobRepository jobRepository;

    public void processJobs() {

        List<AsyncJob> jobs =
            jobRepository.findTop10ByStatusOrderByCreatedAt("PENDING");
        
        for (AsyncJob job : jobs) {
            process(job);
            job.setStatus("DONE");
            jobRepository.save(job);
        }
    }

    private void process(AsyncJob job) {
        switch (job.getType()) {
            case "SEND_EMAIL":
                break;
            case "GENERATE_REPORT":
                break;
            default:
                break;
        }
    }
    
}
