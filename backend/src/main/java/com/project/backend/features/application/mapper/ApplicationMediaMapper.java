package com.project.backend.features.application.mapper;

import java.time.YearMonth;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.project.backend.features.application.dto.ApplicationMediaCreateRequest;
import com.project.backend.features.application.dto.ApplicationMediaDetailResponse;
import com.project.backend.features.application.dto.ApplicationMediaListItemResponse;
import com.project.backend.features.application.dto.ApplicationMediaUpdateRequest;
import com.project.backend.features.application.entity.ApplicationMedia;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ApplicationMediaMapper {

    @Mapping(target = "mediaYearMonth", expression = "java(toYearMonthString(entity.getMediaYearMonth()))")
    ApplicationMediaListItemResponse toListItem(ApplicationMedia entity);

    @Mapping(target = "mediaYearMonth", expression = "java(toYearMonthString(entity.getMediaYearMonth()))")
    ApplicationMediaDetailResponse toDetail(ApplicationMedia entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hires", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(
        target = "mediaYearMonth",
        expression = "java(toYearMonth(request.getMediaYearMonth()))"
    )
    ApplicationMedia toEntity(ApplicationMediaCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "hires", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(
        target = "mediaYearMonth",
        expression = "java(resolveYearMonth(entity.getMediaYearMonth(), request.getMediaYearMonth()))"
    )
    void update(@MappingTarget ApplicationMedia entity, ApplicationMediaUpdateRequest request);

    default String toYearMonthString(YearMonth value) {
        return value == null ? null : value.toString();
    }

    default YearMonth toYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return YearMonth.parse(value);
    }

    default YearMonth resolveYearMonth(YearMonth currentValue, String requestValue) {
        if (requestValue == null || requestValue.isBlank()) {
            return currentValue;
        }
        return YearMonth.parse(requestValue);
    }
}
