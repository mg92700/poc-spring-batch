package com.poc_spring_batch.service;

import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.repository.FileRecordRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class BatchService {

    private final JobOperator jobOperator;
    private final Job scanDirectoryJob;
    private final FileRecordRepository repository;

    public BatchService(JobOperator jobOperator, Job scanDirectoryJob, FileRecordRepository repository) {
        this.jobOperator = jobOperator;
        this.scanDirectoryJob = scanDirectoryJob;
        this.repository = repository;
    }

    public JobResult launch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution exec = jobOperator.start(scanDirectoryJob, params);
            log.info("Job terminé avec le statut : {}", exec.getStatus());

            return new JobResult(
                    exec.getJobInstanceId(),
                    exec.getStatus().name(),
                    exec.getStartTime(),
                    exec.getEndTime(),
                    exec.getExitStatus().getExitCode(),
                    null
            );

        } catch (Exception e) {
            log.error("Erreur lors du lancement du job : {}", e.getMessage());
            return new JobResult(null, "ERROR", null, null, "FAILED", e.getMessage());
        }
    }

    public List<FileRecord> getAllRecords() {
        return repository.findAllOrderByReadAtDesc();
    }

    public Map<String, Long> getStats() {
        return Map.of(
                "total",   repository.count(),
                "success", (long) repository.findByStatus(FileRecord.FileStatus.SUCCESS).size(),
                "error",   (long) repository.findByStatus(FileRecord.FileStatus.ERROR).size()
        );
    }

    public record JobResult(
            Long jobId,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String exitCode,
            String errorMessage
    ) {}
}