package com.poc_spring_batch.job;

import lombok.extern.log4j.Log4j2;

import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemReader;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Lit tous les fichiers du répertoire configuré.
 * Retourne null quand tous les fichiers ont été traités (fin du Step).
 */
@Log4j2
public class FileItemReader implements ItemReader<File> {

    private final String directory;
    private final Deque<File> queue = new ArrayDeque<>();
    private final List<String> processedFileNames = new ArrayList<>();
    private boolean initialized = false;
    private StepExecution stepExecution;

    public FileItemReader(String directory) {
        this.directory = directory;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public File read() {
        if (!initialized) {
            init();
        }

        File next = queue.poll();
        if (next != null) {
            log.debug("Prochain fichier : {}", next.getName());
            // On enregistre chaque fichier au fur et à mesure qu'il est lu
            processedFileNames.add(next.getName());
            stepExecution.getJobExecution()
                    .getExecutionContext()
                    .put("processedFileNames", new ArrayList<>(processedFileNames));
        }
        return next; // null = fin du step
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        log.info("{} fichier(s) transmis à l'étape d'archivage.", processedFileNames.size());
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