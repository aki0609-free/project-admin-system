package com.project.backend.features.system.backup.service.fetcher;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupDataFetcher {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
public List<Map<String, Object>> fetch(
            String sql
    ) {
        return jdbcTemplate.queryForList(
                sql,
                Map.of()
        );
    }
}