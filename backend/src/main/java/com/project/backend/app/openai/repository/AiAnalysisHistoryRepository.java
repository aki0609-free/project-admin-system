package com.project.backend.app.openai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.app.openai.entity.AiAnalysisHistory;

public interface AiAnalysisHistoryRepository extends JpaRepository<AiAnalysisHistory, Long> {
}
