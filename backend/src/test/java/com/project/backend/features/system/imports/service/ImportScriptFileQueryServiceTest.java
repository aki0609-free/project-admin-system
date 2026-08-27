package com.project.backend.features.system.imports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.model.StorageEntry;
import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;

class ImportScriptFileQueryServiceTest {

    @Test
    void findAll_shouldReturnRelativeExecutableScriptPathsOnly() {
        StorageService storageService = mock(StorageService.class);
        StorageProperties properties = new StorageProperties();
        when(storageService.listRecursively("imports/scripts")).thenReturn(List.of(
                entry("imports/scripts/tax/resident.py"),
                entry("imports/scripts/run.sh"),
                entry("imports/scripts/readme.txt")
        ));

        ImportScriptFileQueryService service =
                new ImportScriptFileQueryService(storageService, properties);

        assertThat(service.findAll())
                .extracting(response -> response.filePath())
                .containsExactly("run.sh", "tax/resident.py");
    }

    private StorageEntry entry(String key) {
        return new StorageEntry(
                key,
                key.substring(key.lastIndexOf('/') + 1),
                false,
                1L,
                null,
                null
        );
    }
}
