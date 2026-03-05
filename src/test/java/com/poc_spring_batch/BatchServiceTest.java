package com.poc_spring_batch;

import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.repository.FileRecordRepository;
import com.poc_spring_batch.service.BatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;

import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job scanDirectoryJob;

    @Mock
    private FileRecordRepository repository;

    @InjectMocks
    private BatchService batchService;

    @Test
    void launch_jobReussit_retourneResultatCompleted() throws Exception {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobExecution.getJobInstanceId()).thenReturn(1L);

        doReturn(jobExecution)
                .when(jobOperator)
                .start(any(Job.class), any(JobParameters.class));

        BatchService.JobResult result = batchService.launch();

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.exitCode()).isEqualTo("COMPLETED");
        assertThat(result.errorMessage()).isNull();
        assertThat(result.jobId()).isEqualTo(1L);
    }

    @Test
    void launch_jobEchoue_retourneResultatError() throws Exception {
        doThrow(new RuntimeException("Erreur inattendue"))
                .when(jobOperator)
                .start(any(Job.class), any(JobParameters.class));

        BatchService.JobResult result = batchService.launch();

        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.exitCode()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).contains("Erreur inattendue");
        assertThat(result.jobId()).isNull();
    }
    @Test
    void getAllRecords_retourneListe() {
        List<FileRecord> records = List.of(
                FileRecord.builder().fileName("a.txt").status(FileRecord.FileStatus.SUCCESS).build(),
                FileRecord.builder().fileName("b.txt").status(FileRecord.FileStatus.ERROR).build()
        );
        when(repository.findAllOrderByReadAtDesc()).thenReturn(records);

        List<FileRecord> result = batchService.getAllRecords();

        assertThat(result).hasSize(2);
    }

    @Test
    void getStats_retourneCompteurs() {
        when(repository.count()).thenReturn(10L);
        when(repository.findByStatus(FileRecord.FileStatus.SUCCESS))
                .thenReturn(List.of(FileRecord.builder().build(), FileRecord.builder().build()));
        when(repository.findByStatus(FileRecord.FileStatus.ERROR))
                .thenReturn(List.of(FileRecord.builder().build()));

        Map<String, Long> stats = batchService.getStats();

        assertThat(stats.get("total")).isEqualTo(10L);
        assertThat(stats.get("success")).isEqualTo(2L);
        assertThat(stats.get("error")).isEqualTo(1L);
    }
}