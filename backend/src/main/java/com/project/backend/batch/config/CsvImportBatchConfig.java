package com.project.backend.batch.config;

import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.project.backend.batch.processor.CsvLineToMapProcessor;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportWriteResult;
import com.project.backend.features.system.imports.service.CsvImportWriteService;
import com.project.backend.features.system.imports.service.ImportHistoryService;
import com.project.backend.features.system.imports.service.ImportTargetAdminService;
import com.project.backend.features.system.imports.service.collector.ImportWriteResultCollector;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CsvImportBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CsvImportWriteService csvImportWriteService;
    private final ImportTargetAdminService importTargetAdminService;
    private final ImportHistoryService importHistoryService;
    private final ImportWriteResultCollector resultCollector;

    @SuppressWarnings("null")
    @Bean
    public Job csvImportJob(Step csvImportStep) {
        return new JobBuilder("csvImportJob", jobRepository)
                .start(csvImportStep)
                .build();
    }

    @SuppressWarnings("null")
    @Bean
    public Step csvImportStep(
            ItemReader<String> csvLineReader,
            ItemProcessor<String, Map<String, String>> csvLineProcessor,
            ItemWriter<Map<String, String>> csvRowWriter,
            StepExecutionListener csvImportStepListener
    ) {
        return new StepBuilder("csvImportStep", jobRepository)
                .<String, Map<String, String>>chunk(100, transactionManager)
                .reader(csvLineReader)
                .processor(csvLineProcessor)
                .writer(csvRowWriter)
                .listener(csvImportStepListener)
                .build();
    }

    @Bean
    @StepScope
    public StepExecutionListener csvImportStepListener(
            @Value("#{jobParameters['targetCode']}") String targetCode,
            @Value("#{jobParameters['fileName']}") String fileName
    ) {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(@SuppressWarnings("null") StepExecution stepExecution) {
                csvImportWriteService.prepareBeforeWrite(targetCode);

                stepExecution.getExecutionContext().put(
                        ImportWriteResultCollector.KEY,
                        resultCollector.empty()
                );
            }

            @Override
            public ExitStatus afterStep(@SuppressWarnings("null") StepExecution stepExecution) {
                ImportTargetDefinition target = null;

                try {
                    target = importTargetAdminService.findByTargetCode(targetCode);

                    ImportWriteResult result =
                            (ImportWriteResult) stepExecution.getExecutionContext()
                                    .get(ImportWriteResultCollector.KEY);

                    importHistoryService.saveSuccess(
                            target,
                            fileName,
                            stepExecution.getJobExecutionId(),
                            result
                    );

                } catch (Exception e) {
                    importHistoryService.saveFailure(
                            target,
                            fileName,
                            stepExecution.getJobExecutionId(),
                            e
                    );

                    return ExitStatus.FAILED;
                }

                return stepExecution.getExitStatus();
            }
        };
    }

    @SuppressWarnings("null")
    @Bean
    @StepScope
    public FlatFileItemReader<String> csvLineReader(
            @Value("#{jobParameters['filePath']}") String filePath
    ) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);
        reader.setLineMapper(new PassThroughLineMapper());
        reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<String, Map<String, String>> csvLineProcessor(
            @Value("#{jobParameters['filePath']}") String filePath
    ) {
        return new CsvLineToMapProcessor(filePath);
    }

    @Bean
    @StepScope
    public ItemWriter<Map<String, String>> csvRowWriter(
            @Value("#{jobParameters['targetCode']}") String targetCode
    ) {
        return rows -> {
            ImportWriteResult chunkResult =
                    csvImportWriteService.writeRows(
                            targetCode,
                            rows.getItems()
                    );

            @SuppressWarnings("null")
            ExecutionContext executionContext =
                    StepSynchronizationManager.getContext()
                            .getStepExecution()
                            .getExecutionContext();

            ImportWriteResult current =
                    (ImportWriteResult) executionContext.get(
                            ImportWriteResultCollector.KEY
                    );

            executionContext.put(
                    ImportWriteResultCollector.KEY,
                    resultCollector.merge(
                            current,
                            chunkResult
                    )
            );
        };
    }
}