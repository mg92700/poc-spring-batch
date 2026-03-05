package com.poc_spring_batch.job;

import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Component
public class ArchiveFileTasklet implements Tasklet {

    @Value("${batch.input.directory}")
    private String inputDirectory;

    @Value("${batch.archive.directory}")
    private String archiveDirectory;


    @Override
    public @Nullable RepeatStatus execute(org.springframework.batch.core.step.StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<String> fileNames = (List<String>) chunkContext
                .getStepContext()
                .getJobExecutionContext()
                .get("processedFileNames");

        if (fileNames == null || fileNames.isEmpty()) {
            log.warn("Aucun fichier à archiver.");
            return RepeatStatus.FINISHED;
        }

        Path archiveDir = Paths.get(archiveDirectory);
        Files.createDirectories(archiveDir);

        for (String fileName : fileNames) {
            Path source = Paths.get(inputDirectory, fileName);

            if (!Files.exists(source)) {
                log.warn("Fichier source introuvable, ignoré : {}", source);
                continue;
            }

            String zipName = fileName.replaceAll("\\.[^.]+$", "") + ".zip";
            Path zipTarget = archiveDir.resolve(zipName);

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipTarget.toFile()))) {
                zos.putNextEntry(new ZipEntry(fileName));
                Files.copy(source, zos);
                zos.closeEntry();
            }

            Files.delete(source);
            log.info("Archivé : {} → {}", source, zipTarget);
        }

        return RepeatStatus.FINISHED;
    }
}