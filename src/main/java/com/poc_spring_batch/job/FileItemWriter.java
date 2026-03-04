package com.poc_spring_batch.job;


import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste les FileRecord en base H2.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class FileItemWriter implements ItemWriter<FileRecord> {

    private final FileRecordRepository repository;

    @Override
    public void write(Chunk<? extends FileRecord> chunk) {
        repository.saveAll(chunk.getItems());
        chunk.getItems().forEach(r ->
                log.info("✔ Persisté | {} | titre='{}' | lu à {}",
                        r.getFileName(), r.getTitle(), r.getReadAt())
        );
    }
}
