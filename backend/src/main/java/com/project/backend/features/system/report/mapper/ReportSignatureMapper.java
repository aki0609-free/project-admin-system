package com.project.backend.features.system.report.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.project.backend.common.util.ApplicationBase64Utils;
import com.project.backend.features.system.report.dto.ReportSignatureResponse;
import com.project.backend.features.system.report.entity.ReportSignature;

@Mapper(componentModel = "spring", imports = ApplicationBase64Utils.class)
public interface ReportSignatureMapper {

    @Mapping(target = "reportMasterId", source = "reportMaster.id")
    @Mapping(
            target = "signatureImageBase64",
            expression = "java(ApplicationBase64Utils.encodeDataUrl(entity.getSignatureImageData(), entity.getContentType()))"
    )
    ReportSignatureResponse toResponse(ReportSignature entity);

    List<ReportSignatureResponse> toResponseList(List<ReportSignature> entities);
}