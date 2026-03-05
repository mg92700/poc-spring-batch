package com.poc_spring_batch.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArchiveFileTaskletTest {

    @TempDir
    Path inputDir;

    @TempDir
    Path archiveDir;

    private ArchiveFileTasklet tasklet;
    private StepContribution contribution;
    private ChunkContext chunkContext;
    private StepContext stepContext;

    @BeforeEach
    void setUp() {
        tasklet = new ArchiveFileTasklet();
        ReflectionTestUtils.setField(tasklet, "inputDirectory", inputDir.toString());
        ReflectionTestUtils.setField(tasklet, "archiveDirectory", archiveDir.toString());

        contribution = mock(StepContribution.class);
        chunkContext  = mock(ChunkContext.class);
        stepContext   = mock(StepContext.class);

        when(chunkContext.getStepContext()).thenReturn(stepContext);
    }

    @Test
    void execute_aucunFichierDansContexte_retourneFinished() throws Exception {
        when(stepContext.getJobExecutionContext()).thenReturn(Map.of());

        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void execute_fichierPresent_estZippeEtSupprime() throws Exception {
        // Crée un fichier source
        Path source = inputDir.resolve("rapport.csv");
        Files.writeString(source, "col1,col2\nval1,val2");

        when(stepContext.getJobExecutionContext())
                .thenReturn(Map.of("processedFileNames", List.of("rapport.csv")));

        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        // fichier original supprimé
        assertThat(Files.exists(source)).isFalse();
        // zip créé
        Path zip = archiveDir.resolve("rapport.zip");
        assertThat(Files.exists(zip)).isTrue();
        // zip contient le bon fichier
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            assertThat(zf.getEntry("rapport.csv")).isNotNull();
        }
    }

    @Test
    void execute_fichierAbsent_skipSansErreur() throws Exception {
        when(stepContext.getJobExecutionContext())
                .thenReturn(Map.of("processedFileNames", List.of("inexistant.csv")));

        RepeatStatus status = tasklet.execute(contribution, chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        assertThat(Files.list(archiveDir).count()).isZero();
    }

    @Test
    void execute_plusieursFichiers_tousArchives() throws Exception {
        Files.writeString(inputDir.resolve("a.txt"), "contenu a");
        Files.writeString(inputDir.resolve("b.txt"), "contenu b");

        when(stepContext.getJobExecutionContext())
                .thenReturn(Map.of("processedFileNames", List.of("a.txt", "b.txt")));

        tasklet.execute(contribution, chunkContext);

        assertThat(Files.exists(inputDir.resolve("a.txt"))).isFalse();
        assertThat(Files.exists(inputDir.resolve("b.txt"))).isFalse();
        assertThat(Files.exists(archiveDir.resolve("a.zip"))).isTrue();
        assertThat(Files.exists(archiveDir.resolve("b.zip"))).isTrue();
    }
}