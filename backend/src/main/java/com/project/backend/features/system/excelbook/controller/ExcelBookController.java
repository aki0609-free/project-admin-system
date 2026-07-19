package com.project.backend.features.system.excelbook.controller;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.excelbook.dto.ExcelBookUpdateRequest;
import com.project.backend.features.system.excelbook.service.ExcelBookUpdateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/excel-books")
@RequiredArgsConstructor
public class ExcelBookController {

    private final ExcelBookUpdateService service;

    @SuppressWarnings("null")
    @PostMapping("/{bookCode}/update")
    public ResponseEntity<InputStreamResource> update(
            @PathVariable String bookCode,
            @RequestBody ExcelBookUpdateRequest request
    ) throws Exception {
        Path path = service.updateBook(bookCode, request.targetMonth());

        String fileName = URLEncoder.encode(
                path.getFileName().toString(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        InputStream inputStream = Files.newInputStream(path);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + fileName
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(Files.size(path))
                .body(new InputStreamResource(inputStream));
    }
}