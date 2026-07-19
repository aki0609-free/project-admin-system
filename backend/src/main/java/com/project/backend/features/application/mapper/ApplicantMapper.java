package com.project.backend.features.application.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.project.backend.features.application.dto.ApplicantCreateRequest;
import com.project.backend.features.application.dto.ApplicantDetailResponse;
import com.project.backend.features.application.dto.ApplicantListItemResponse;
import com.project.backend.features.application.dto.ApplicantUpdateRequest;
import com.project.backend.features.application.dto.ApplicationMediaLinkResponse;
import com.project.backend.features.application.dto.DormitoryInfoRequest;
import com.project.backend.features.application.dto.DormitoryInfoResponse;
import com.project.backend.features.application.dto.PreviousJobInfoRequest;
import com.project.backend.features.application.dto.PreviousJobInfoResponse;
import com.project.backend.features.application.dto.SearchKeywordInfoRequest;
import com.project.backend.features.application.dto.SearchKeywordInfoResponse;
import com.project.backend.features.application.entity.Applicant;
import com.project.backend.features.application.entity.ApplicationMedia;
import com.project.backend.features.application.entity.DormitoryInfo;
import com.project.backend.features.application.entity.PreviousJobInfo;
import com.project.backend.features.application.entity.SearchKeywordInfo;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ApplicantMapper {

    @Mapping(target = "applicationMedia", source = "applicationMedia")
    ApplicantListItemResponse toListItem(Applicant entity);

    @Mapping(target = "applicationMedia", source = "applicationMedia")
    ApplicantDetailResponse toDetail(Applicant entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationMedia", ignore = true)
    @Mapping(target = "mediaNameSnapshot", ignore = true)
    @Mapping(target = "mediaYearMonthSnapshot", ignore = true)
    Applicant toEntity(ApplicantCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "applicationMedia", ignore = true)
    @Mapping(target = "mediaNameSnapshot", ignore = true)
    @Mapping(target = "mediaYearMonthSnapshot", ignore = true)
    void update(@MappingTarget Applicant entity, ApplicantUpdateRequest request);

    DormitoryInfo toDormitoryInfo(DormitoryInfoRequest request);

    SearchKeywordInfo toSearchKeywordInfo(SearchKeywordInfoRequest request);

    PreviousJobInfo toPreviousJobInfo(PreviousJobInfoRequest request);

    DormitoryInfoResponse toDormitoryInfoResponse(DormitoryInfo entity);

    SearchKeywordInfoResponse toSearchKeywordInfoResponse(SearchKeywordInfo entity);

    PreviousJobInfoResponse toPreviousJobInfoResponse(PreviousJobInfo entity);

    @Mapping(target = "mediaYearMonth", expression = "java(toYearMonthString(entity.getMediaYearMonth()))")
    ApplicationMediaLinkResponse toApplicationMediaLinkResponse(ApplicationMedia entity);

    default String toYearMonthString(java.time.YearMonth value) {
        return value == null ? null : value.toString();
    }
}