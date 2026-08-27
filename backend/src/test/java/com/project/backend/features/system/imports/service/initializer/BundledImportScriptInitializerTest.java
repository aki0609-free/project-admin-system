package com.project.backend.features.system.imports.service.initializer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;

class BundledImportScriptInitializerTest {

    @Test
    void run_shouldInitializeAllMissingScripts() {
        StorageService storageService = mock(StorageService.class);
        when(storageService.exists(anyString())).thenReturn(false);
        BundledImportScriptInitializer initializer =
                new BundledImportScriptInitializer(
                        storageService,
                        new StorageProperties()
                );

        initializer.run(null);

        verify(storageService, times(9)).save(
                anyString(),
                any(InputStream.class),
                anyLong(),
                org.mockito.ArgumentMatchers.eq(
                        "text/x-python; charset=UTF-8"
                )
        );
    }

    @Test
    void run_shouldNotOverwriteExistingScripts() {
        StorageService storageService = mock(StorageService.class);
        when(storageService.exists(anyString())).thenReturn(true);
        BundledImportScriptInitializer initializer =
                new BundledImportScriptInitializer(
                        storageService,
                        new StorageProperties()
                );

        initializer.run(null);

        verify(storageService, never()).save(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        );
    }
}
