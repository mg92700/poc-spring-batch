package com.poc_spring_batch.job;




import com.poc_spring_batch.domain.FileRecord;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

/**
 * Configuration du Job "scanDirectoryJob".
 * Le répertoire est lu depuis batch.input.directory dans application.properties.
 */
@Log4j2
@Configuration
public class BatchConfig {

    @Value("${batch.input.directory:/tmp/batch-input}")
    private String inputDirectory;

    private final FileItemProcessor processor;
    private final FileItemWriter writer;

    public BatchConfig(FileItemProcessor processor, FileItemWriter writer) {
        this.processor = processor;
        this.writer = writer;
    }

    @Bean
    public Job scanDirectoryJob(JobRepository jobRepository, Step scanStep) {
        return new JobBuilder("scanDirectoryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(scanStep)
                .build();
    }

    @Bean
    public Step scanStep(JobRepository jobRepository,
                         PlatformTransactionManager txManager) {
        return new StepBuilder("scanStep", jobRepository)
                .<File, FileRecord>chunk(new SimpleCompletionPolicy(10), txManager)
                .reader(fileItemReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public FileItemReader fileItemReader() {
        log.info("Répertoire batch : {}", inputDirectory);
        return new FileItemReader(inputDirectory);
    }
}
