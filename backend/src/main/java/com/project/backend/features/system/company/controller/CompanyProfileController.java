package com.project.backend.features.system.company.controller;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.company.dto.CompanyProfileResponse;
import com.project.backend.features.system.company.dto.CompanyProfileSaveRequest;
import com.project.backend.features.system.company.entity.CompanyProfile;
import com.project.backend.features.system.company.service.CompanyProfileCommandService;
import com.project.backend.features.system.company.service.CompanyProfileQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/company-profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private static final String DEFAULT_INVOICE_LOGO =
            "reports/assets/company_invoice_logo.png";

    private final CompanyProfileQueryService queryService;
    private final CompanyProfileCommandService commandService;

    @GetMapping
    public CompanyProfileResponse findCurrent() {
        return queryService.findCurrentOrNull();
    }

    @PutMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public CompanyProfileResponse save(
            @RequestBody CompanyProfileSaveRequest request
    ) {
        return commandService.save(request);
    }

    @GetMapping("/invoice-logo")
    public ResponseEntity<byte[]> findInvoiceLogo() throws IOException {
        CompanyProfile profile = queryService.findCurrentEntityOrNull();
        byte[] imageData = profile != null
                ? profile.getInvoiceLogoImageData()
                : null;
        String contentType = profile != null
                ? profile.getInvoiceLogoContentType()
                : null;

        if (imageData == null || imageData.length == 0) {
            ClassPathResource fallback = new ClassPathResource(DEFAULT_INVOICE_LOGO);
            try (var inputStream = fallback.getInputStream()) {
                imageData = inputStream.readAllBytes();
            }
            contentType = MediaType.IMAGE_PNG_VALUE;
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(
                    contentType != null ? contentType : MediaType.IMAGE_PNG_VALUE
            );
        } catch (IllegalArgumentException ignored) {
            mediaType = MediaType.IMAGE_PNG;
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(mediaType)
                .body(imageData);
    }
}
