package com.poc_spring_batch.job;


import com.poc_spring_batch.domain.FileRecord;
import lombok.extern.log4j.Log4j2;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait le titre du fichier selon son type :
 *   .md / .txt  → cherche "# Titre" ou "title: ..."  ou première ligne
 *   .html       → extrait <title>...</title>
 *   autre       → nom du fichier sans extension
 */
@Log4j2
@Component
public class FileItemProcessor implements ItemProcessor<File, FileRecord> {

    @Override
    public FileRecord process(File file) {
        log.info("Traitement : {}", file.getName());
        String title = extractTitle(file);

        return FileRecord.builder()
                .fileName(file.getName())
                .filePath(file.getAbsolutePath())
                .title(file.getName())
                .readAt(LocalDateTime.now())
                .fileSizeBytes(file.length())
                .status(FileRecord.FileStatus.SUCCESS)
                .build();
    }

    // ---------------------------------------------------------------

    private String extractTitle(File file) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".html") || name.endsWith(".htm")) {
                return htmlTitle(file);
            }
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                return textTitle(file);
            }
        } catch (IOException e) {
            log.warn("Impossible de lire {} : {}", file.getName(), e.getMessage());
        }
        return withoutExtension(file.getName());
    }

    private String htmlTitle(File file) throws IOException {
        String content = Files.readString(file.toPath());
        Matcher m = Pattern.compile("<title[^>]*>(.*?)</title>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content);
        return m.find() ? m.group(1).trim() : withoutExtension(file.getName());
    }

    private String textTitle(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line : lines) {
            if (line.startsWith("# "))              return line.substring(2).trim();
            if (line.toLowerCase().startsWith("title:")) return line.substring(6).trim();
            if (!line.isBlank())                    return truncate(line.trim());
        }
        return withoutExtension(file.getName());
    }

    private String withoutExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String truncate(String s) {
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }
}
