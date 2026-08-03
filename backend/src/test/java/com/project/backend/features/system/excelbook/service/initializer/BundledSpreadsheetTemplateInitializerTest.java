package com.project.backend.features.system.excelbook.service.initializer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;

class BundledSpreadsheetTemplateInitializerTest {

    private static final String RECEIPT_STORAGE_KEY =
            "documents/templates/ledgers/default/"
                    + "RECEIPT_CONFIRMATION/template.json";
    private static final String MONTHLY_SUMMARY_STORAGE_KEY =
            "documents/templates/ledgers/default/"
                    + "MONTHLY_SUMMARY/template.json";

    private final StorageService storageService =
            org.mockito.Mockito.mock(StorageService.class);
    private final BundledSpreadsheetTemplateInitializer initializer =
            new BundledSpreadsheetTemplateInitializer(
                    storageService,
                    new DocumentStorageKeyResolver(
                            new StorageProperties()
                    )
            );

    @Test
    void savesBundledTemplateWhenStorageFileDoesNotExist() {
        when(storageService.exists(RECEIPT_STORAGE_KEY)).thenReturn(false);
        when(storageService.exists(MONTHLY_SUMMARY_STORAGE_KEY))
                .thenReturn(false);

        initializer.run(new DefaultApplicationArguments());

        verify(storageService).save(
                eq(RECEIPT_STORAGE_KEY),
                isA(InputStream.class),
                longThat(size -> size > 0),
                eq("application/json")
        );
        verify(storageService).save(
                eq(MONTHLY_SUMMARY_STORAGE_KEY),
                isA(InputStream.class),
                longThat(size -> size > 0),
                eq("application/json")
        );
    }

    @Test
    void preservesExistingStorageTemplate() {
        when(storageService.exists(RECEIPT_STORAGE_KEY)).thenReturn(true);
        when(storageService.exists(MONTHLY_SUMMARY_STORAGE_KEY))
                .thenReturn(true);

        initializer.run(new DefaultApplicationArguments());

        verify(storageService, never()).save(
                eq(RECEIPT_STORAGE_KEY),
                isA(InputStream.class),
                org.mockito.ArgumentMatchers.anyLong(),
                eq("application/json")
        );
        verify(storageService, never()).save(
                eq(MONTHLY_SUMMARY_STORAGE_KEY),
                isA(InputStream.class),
                org.mockito.ArgumentMatchers.anyLong(),
                eq("application/json")
        );
    }
}
