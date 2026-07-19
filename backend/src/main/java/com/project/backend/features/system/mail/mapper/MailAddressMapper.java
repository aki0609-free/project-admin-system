package com.project.backend.features.system.mail.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MailAddressMapper {

    @Named("joinAddresses")
    public String joinAddresses(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        String joined = addresses.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));

        return StringUtils.hasText(joined) ? joined : null;
    }

    @Named("splitAddresses")
    public List<String> splitAddresses(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        return Arrays.stream(value.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}