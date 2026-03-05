package com.poc_spring_batch.job;

import lombok.extern.log4j.Log4j2;

import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.repository.FileRecordRepository;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
public class FileItemWriter implements ItemWriter<FileRecord> {

    @Autowired
    private FileRecordRepository repository;

    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @SuppressWarnings("unchecked")
    private List<String> getOrInitList() {
        List<String> existing = (List<String>) stepExecution
                .getJobExecution()
                .getExecutionContext()
                .get("processedFileNames");
        return existing != null ? existing : new ArrayList<>();
    }

    @Override
    public void write(Chunk<? extends FileRecord> items) throws Exception {
        repository.saveAll(items);
        log.info("{} enregistrement(s) sauvegardés.", items.size());

        List<String> processedFileNames = getOrInitList();
        items.getItems().stream()
                .map(FileRecord::getFileName)  // ← fileName au lieu de sourceFileName
                .filter(name -> !processedFileNames.contains(name))
                .forEach(processedFileNames::add);

        stepExecution.getJobExecution()
                .getExecutionContext()
                .put("processedFileNames", new ArrayList<>(processedFileNames));
    }
}