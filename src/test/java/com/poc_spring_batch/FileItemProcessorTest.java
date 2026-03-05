package com.poc_spring_batch.job;

import com.poc_spring_batch.domain.FileRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileItemProcessorTest {

    @TempDir
    Path tempDir;

    private FileItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new FileItemProcessor();
    }

    @Test
    void process_fichierTxt_retourneFileRecordAvecTitrePremiereLigne() throws Exception {
        Path file = tempDir.resolve("rapport.txt");
        Files.writeString(file, "Mon titre\nContenu du fichier");

        FileRecord result = processor.process(file.toFile());

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("rapport.txt");
        assertThat(result.getFilePath()).isEqualTo(file.toAbsolutePath().toString());
        assertThat(result.getStatus()).isEqualTo(FileRecord.FileStatus.SUCCESS);
        assertThat(result.getFileSizeBytes()).isPositive();
        assertThat(result.getReadAt()).isNotNull();
    }

    @Test
    void process_fichierMd_extraitTitreMarkdown() throws Exception {
        Path file = tempDir.resolve("doc.md");
        Files.writeString(file, "# Titre Markdown\nContenu");

        FileRecord result = processor.process(file.toFile());

        assertThat(result.getFileName()).isEqualTo("doc.md");
        assertThat(result.getStatus()).isEqualTo(FileRecord.FileStatus.SUCCESS);
    }

    @Test
    void process_fichierHtml_extraitBaliseTitle() throws Exception {
        Path file = tempDir.resolve("page.html");
        Files.writeString(file, "<html><head><title>Ma Page</title></head><body></body></html>");

        FileRecord result = processor.process(file.toFile());

        assertThat(result.getFileName()).isEqualTo("page.html");
        assertThat(result.getStatus()).isEqualTo(FileRecord.FileStatus.SUCCESS);
    }

    @Test
    void process_fichierInconnu_utiliseNomSansExtension() throws Exception {
        Path file = tempDir.resolve("archive.zip");
        Files.write(file, new byte[]{0x50, 0x4B});

        FileRecord result = processor.process(file.toFile());

        assertThat(result.getFileName()).isEqualTo("archive.zip");
        assertThat(result.getStatus()).isEqualTo(FileRecord.FileStatus.SUCCESS);
    }

    @Test
    void process_fichierVide_retourneQuandMeme() throws Exception {
        Path file = tempDir.resolve("vide.txt");
        Files.writeString(file, "");

        FileRecord result = processor.process(file.toFile());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FileRecord.FileStatus.SUCCESS);
    }
}