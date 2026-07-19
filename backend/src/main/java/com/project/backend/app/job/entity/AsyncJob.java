package com.project.backend.app.job.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="async_job")
@Getter
@Setter
public class AsyncJob {

    @Id
    @GeneratedValue
    private Long id;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status;

    private Instant createdAt;
    
}
