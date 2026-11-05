package com.project.backend.features.system.masterdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.masterdata.dto.GenericMasterDefinitionSaveRequest;
import com.project.backend.features.system.masterdata.dto.GenericMasterFieldRequest;
import com.project.backend.features.system.masterdata.dto.GenericMasterRecordSaveRequest;
import com.project.backend.features.system.masterdata.entity.GenericMasterDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterFieldDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterFieldType;
import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;
import com.project.backend.features.system.masterdata.entity.GenericMasterRecord;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;
import com.project.backend.features.system.masterdata.repository.GenericMasterDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterFieldDefinitionRepository;
import com.project.backend.features.system.masterdata.repository.GenericMasterRecordRepository;
import com.project.backend.features.system.masterdata.contract.GenericMasterContractRegistry;
import com.project.backend.features.system.masterdata.contract.GenericMasterSynchronizerRegistry;

class GenericMasterServiceTest {

    private GenericMasterDefinitionRepository definitionRepository;
    private GenericMasterFieldDefinitionRepository fieldRepository;
    private GenericMasterRecordRepository recordRepository;
    private GenericMasterService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        definitionRepository = mock(GenericMasterDefinitionRepository.class);
        fieldRepository = mock(GenericMasterFieldDefinitionRepository.class);
        recordRepository = mock(GenericMasterRecordRepository.class);
        service = new GenericMasterService(
                definitionRepository,
                fieldRepository,
                recordRepository,
                mock(GenericMasterContractRegistry.class),
                mock(GenericMasterSynchronizerRegistry.class),
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createDefinition_shouldCreateDraftWithFields() {
        when(definitionRepository.existsByTenantIdAndMasterCodeAndDeletedAtIsNull(
                "tenant-test", "CUSTOM_POLICY"
        )).thenReturn(false);
        when(definitionRepository.save(any(GenericMasterDefinition.class)))
                .thenAnswer(invocation -> {
                    GenericMasterDefinition definition = invocation.getArgument(0);
                    definition.setId(10L);
                    return definition;
                });
        when(fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "tenant-test", 10L
                )).thenReturn(List.of());

        var response = service.createDefinition(definitionRequest());

        assertThat(response.status()).isEqualTo(GenericMasterStatus.DRAFT);
        assertThat(response.masterCode()).isEqualTo("CUSTOM_POLICY");
        verify(fieldRepository).save(any(GenericMasterFieldDefinition.class));
    }

    @Test
    void publish_shouldExposeDefinition() {
        GenericMasterDefinition definition = definition(10L, GenericMasterStatus.DRAFT);
        GenericMasterFieldDefinition field = field();
        when(definitionRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                10L, "tenant-test"
        )).thenReturn(Optional.of(definition));
        when(fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "tenant-test", 10L
                )).thenReturn(List.of(field));

        var response = service.publish(10L);

        assertThat(response.status()).isEqualTo(GenericMasterStatus.PUBLISHED);
        assertThat(response.activeFlag()).isTrue();
    }

    @Test
    void createRecord_shouldValidateRequiredValuesAndPersistJson() {
        GenericMasterDefinition definition = definition(10L, GenericMasterStatus.PUBLISHED);
        GenericMasterFieldDefinition field = field();
        when(definitionRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                10L, "tenant-test"
        )).thenReturn(Optional.of(definition));
        when(fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "tenant-test", 10L
                )).thenReturn(List.of(field));
        when(recordRepository.save(any(GenericMasterRecord.class)))
                .thenAnswer(invocation -> {
                    GenericMasterRecord record = invocation.getArgument(0);
                    record.setId(20L);
                    return record;
                });

        var response = service.createRecord(
                10L,
                new GenericMasterRecordSaveRequest(
                        Map.of("POLICY_NAME", "標準制度"), true, null
                )
        );

        assertThat(response.values()).containsEntry("POLICY_NAME", "標準制度");
        verify(recordRepository).save(any(GenericMasterRecord.class));
    }

    @Test
    void createRecord_shouldRejectMissingRequiredValue() {
        GenericMasterDefinition definition = definition(10L, GenericMasterStatus.PUBLISHED);
        when(definitionRepository.findByIdAndTenantIdAndDeletedAtIsNull(
                10L, "tenant-test"
        )).thenReturn(Optional.of(definition));
        when(fieldRepository
                .findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "tenant-test", 10L
                )).thenReturn(List.of(field()));

        assertThatThrownBy(() -> service.createRecord(
                10L,
                new GenericMasterRecordSaveRequest(Map.of(), true, null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("制度名は必須");
    }

    private GenericMasterDefinitionSaveRequest definitionRequest() {
        return new GenericMasterDefinitionSaveRequest(
                "CUSTOM_POLICY",
                "会社別制度",
                "会社別制度",
                GenericMasterPlacement.BUSINESS_SETTINGS,
                "会社ごとの制度を管理します。",
                100,
                true,
                List.of(new GenericMasterFieldRequest(
                        "POLICY_NAME", "制度名", GenericMasterFieldType.STRING,
                        true, true, true, 10,
                        null, null, null, null
                ))
        );
    }

    private GenericMasterDefinition definition(Long id, GenericMasterStatus status) {
        GenericMasterDefinition definition = new GenericMasterDefinition();
        definition.setId(id);
        definition.setMasterCode("CUSTOM_POLICY");
        definition.setMasterName("会社別制度");
        definition.setTabName("会社別制度");
        definition.setPlacement(GenericMasterPlacement.BUSINESS_SETTINGS);
        definition.setDisplayOrder(100);
        definition.setStatus(status);
        definition.setActiveFlag(true);
        return definition;
    }

    private GenericMasterFieldDefinition field() {
        GenericMasterFieldDefinition field = new GenericMasterFieldDefinition();
        field.setId(1L);
        field.setDefinitionId(10L);
        field.setFieldKey("POLICY_NAME");
        field.setFieldLabel("制度名");
        field.setFieldType(GenericMasterFieldType.STRING);
        field.setRequiredFlag(true);
        field.setListVisibleFlag(true);
        field.setSearchableFlag(true);
        field.setDisplayOrder(10);
        return field;
    }
}
