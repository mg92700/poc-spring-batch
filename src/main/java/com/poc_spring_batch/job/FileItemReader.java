package com.poc_spring_batch.job;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.infrastructure.item.ItemReader;


import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Lit tous les fichiers du répertoire configuré.
 * Retourne null quand tous les fichiers ont été traités (fin du Step).
 */
@Log4j2
public class FileItemReader implements ItemReader<File> {

    private final String directory;
    private final Deque<File> queue = new ArrayDeque<>();
    private boolean initialized = false;

    public FileItemReader(String directory) {
        this.directory = directory;
    }

    @Override
    public File read() {
        if (!initialized) {
            init();
        }
        File next = queue.poll();
        if (next != null) {
            log.debug("Prochain fichier : {}", next.getName());
        }
        return next; // null = fin du step
    }

    private void init() {
        log.info("Scan du répertoire : {}", directory);
        File dir = new File(directory);

        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Répertoire introuvable ou invalide : {}", directory);
            initialized = true;
            return;
        }

        File[] files = dir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            log.info("Aucun fichier dans : {}", directory);
        } else {
            Arrays.stream(files)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .forEach(queue::add);
            log.info("{} fichier(s) trouvé(s)", queue.size());
        }

        initialized = true;
    }
}
