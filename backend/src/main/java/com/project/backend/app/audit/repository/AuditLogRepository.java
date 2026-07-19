package com.project.backend.app.audit.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.project.backend.app.audit.entity.AuditLog;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

}