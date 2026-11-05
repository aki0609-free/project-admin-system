package com.project.backend.features.system.settings.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.system.settings.entity.SystemSettingDefinition;

public interface SystemSettingDefinitionRepository
        extends JpaRepository<SystemSettingDefinition, Long> {

    @Query("""
            SELECT definition
            FROM SystemSettingDefinition definition
            WHERE definition.activeFlag = true
              AND definition.deletedAt IS NULL
              AND (
                    definition.scope = com.project.backend.features.system.settings.entity.SystemSettingScope.SYSTEM
                    OR (
                        definition.scope = com.project.backend.features.system.settings.entity.SystemSettingScope.TENANT
                        AND definition.ownerTenantId = :tenantId
                    )
              )
            ORDER BY definition.displayOrder, definition.id
            """)
    List<SystemSettingDefinition> findEffective(@Param("tenantId") String tenantId);

    @Query("""
            SELECT definition
            FROM SystemSettingDefinition definition
            WHERE definition.deletedAt IS NULL
              AND (
                    definition.scope = com.project.backend.features.system.settings.entity.SystemSettingScope.SYSTEM
                    OR (
                        definition.scope = com.project.backend.features.system.settings.entity.SystemSettingScope.TENANT
                        AND definition.ownerTenantId = :tenantId
                    )
              )
            ORDER BY definition.displayOrder, definition.id
            """)
    List<SystemSettingDefinition> findManageable(@Param("tenantId") String tenantId);

    Optional<SystemSettingDefinition> findByLargeCategoryKeyAndMiddleCategoryKeyAndSmallCategoryKeyAndScopeAndActiveFlagTrueAndDeletedAtIsNull(
            String largeCategoryKey,
            String middleCategoryKey,
            String smallCategoryKey,
            com.project.backend.features.system.settings.entity.SystemSettingScope scope
    );

    @Query("""
            SELECT CASE WHEN COUNT(definition) > 0 THEN true ELSE false END
            FROM SystemSettingDefinition definition
            WHERE definition.deletedAt IS NULL
              AND definition.largeCategoryKey = :largeKey
              AND definition.middleCategoryKey = :middleKey
              AND definition.smallCategoryKey = :smallKey
              AND (
                    definition.scope = com.project.backend.features.system.settings.entity.SystemSettingScope.SYSTEM
                    OR definition.ownerTenantId = :tenantId
              )
            """)
    boolean existsEffectiveKey(
            @Param("tenantId") String tenantId,
            @Param("largeKey") String largeKey,
            @Param("middleKey") String middleKey,
            @Param("smallKey") String smallKey
    );

    Optional<SystemSettingDefinition> findByIdAndScopeAndOwnerTenantIdAndDeletedAtIsNull(
            Long id,
            com.project.backend.features.system.settings.entity.SystemSettingScope scope,
            String ownerTenantId
    );

    Optional<SystemSettingDefinition> findByLargeCategoryKeyAndMiddleCategoryKeyAndSmallCategoryKeyAndScopeAndOwnerTenantId(
            String largeCategoryKey,
            String middleCategoryKey,
            String smallCategoryKey,
            com.project.backend.features.system.settings.entity.SystemSettingScope scope,
            String ownerTenantId
    );
}
