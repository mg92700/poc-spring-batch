package com.poc_spring_batch.web;


import com.poc_spring_batch.domain.FileRecord;
import com.poc_spring_batch.service.BatchService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints batch — tous protégés par JWT.
 *
 *  POST  /api/batch/launch   → lance le job (ADMIN uniquement)
 *  GET   /api/batch/files    → liste les fichiers traités
 *  GET   /api/batch/stats    → statistiques
 */
@Log4j2
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/launch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchService.JobResult> launch() {
        log.info("Lancement du batch demandé");
        return ResponseEntity.ok(batchService.launch());
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileRecord>> files() {
        return ResponseEntity.ok(batchService.getAllRecords());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(batchService.getStats());
    }
}
