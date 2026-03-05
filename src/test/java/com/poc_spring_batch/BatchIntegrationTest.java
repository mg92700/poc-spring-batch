package com.poc_spring_batch;

import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.repository.FileRecordRepository;
import com.poc_spring_batch.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.poc_spring_batch.PocSpringBatchApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // ← recrée le contexte après chaque test
class BatchIntegrationTest {

    @TempDir
    static Path inputDir;

    @TempDir
    static Path archiveDir;

    @DynamicPropertySource
    static void batchProperties(DynamicPropertyRegistry registry) {
        registry.add("batch.input.directory", inputDir::toString);
        registry.add("batch.archive.directory", archiveDir::toString);
    }

    @Autowired
    private BatchService batchService;

    @Autowired
    private FileRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void jobComplet_fichiersCsvTraitesEtArchives() throws Exception {
        Files.writeString(inputDir.resolve("fichier1.csv"), "col1,col2\nval1,val2");
        Files.writeString(inputDir.resolve("fichier2.txt"), "# Mon Titre\nContenu");
        Files.writeString(inputDir.resolve("fichier3.md"),  "title: Doc Markdown\nTexte");

        BatchService.JobResult result = batchService.launch();

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.exitCode()).isEqualTo("COMPLETED");

        List<FileRecord> records = repository.findAll();
        assertThat(records).hasSize(3);
        assertThat(records).allMatch(r -> r.getStatus() == FileRecord.FileStatus.SUCCESS);

        assertThat(Files.exists(inputDir.resolve("fichier1.csv"))).isFalse();
        assertThat(Files.exists(inputDir.resolve("fichier2.txt"))).isFalse();
        assertThat(Files.exists(inputDir.resolve("fichier3.md"))).isFalse();

        assertThat(Files.exists(archiveDir.resolve("fichier1.zip"))).isTrue();
        assertThat(Files.exists(archiveDir.resolve("fichier2.zip"))).isTrue();
        assertThat(Files.exists(archiveDir.resolve("fichier3.zip"))).isTrue();

        try (ZipFile zf = new ZipFile(archiveDir.resolve("fichier1.zip").toFile())) {
            assertThat(zf.getEntry("fichier1.csv")).isNotNull();
        }
    }

    @Test
    void jobRepertoireVide_aucunEnregistrement() throws Exception {
        // Aucun fichier créé — répertoire vide
        BatchService.JobResult result = batchService.launch();

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(repository.findAll()).isEmpty();
        assertThat(Files.list(archiveDir).count()).isZero();
    }
}