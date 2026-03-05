package com.poc_spring_batch.job;

import com.poc_spring_batch.domain.FileRecord;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.IOException;

@Log4j2
@Configuration
public class BatchConfig {

    @Value("${batch.input.directory:/tmp/batch-input}")
    private String inputDirectory;

    private final ArchiveFileTasklet archiveFileTasklet;
    private final FileItemProcessor processor;
    private final FileItemWriter writer;

    public BatchConfig(ArchiveFileTasklet archiveFileTasklet,
                       FileItemProcessor processor,
                       FileItemWriter writer) {
        this.archiveFileTasklet = archiveFileTasklet;
        this.processor = processor;
        this.writer = writer;
    }

    // Un seul Job : scan → archive
    @Bean
    public Job scanDirectoryJob(JobRepository jobRepository,
                                Step scanStep,
                                Step archiveStep) {
        return new JobBuilder("scanDirectoryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(scanStep)
                .next(archiveStep)
                .build();
    }


    /**
     * Définition du Step de scan et traitement des fichiers ({@code scanStep}).
     *
     * <h2>Rôle dans le Job</h2>
     * Ce step constitue la première étape du job {@code scanDirectoryJob}.
     * Il est responsable de :
     * <ol>
     *   <li>Lire les fichiers présents dans le répertoire d'entrée configuré
     *       via {@code batch.input.directory} (défaut : {@code /tmp/batch-input})</li>
     *   <li>Transformer chaque {@link java.io.File} en {@link com.poc_spring_batch.domain.FileRecord}
     *       via le {@link FileItemProcessor}</li>
     *   <li>Persister les {@link com.poc_spring_batch.domain.FileRecord} traités
     *       via le {@link FileItemWriter}</li>
     * </ol>
     *
     * <h2>Mode de traitement : Chunk-Oriented Processing</h2>
     * Le step fonctionne en mode <b>chunk</b> avec une taille de lot de <b>10 éléments</b>.
     * Cela signifie que Spring Batch :
     * <ol>
     *   <li>Lit 10 fichiers un par un via le reader (phase READ)</li>
     *   <li>Transforme chacun via le processor (phase PROCESS)</li>
     *   <li>Écrit les 10 résultats en une seule opération atomique (phase WRITE)</li>
     *   <li>Commite la transaction</li>
     *   <li>Recommence jusqu'à épuisement des fichiers</li>
     * </ol>
     * En cas d'échec lors du WRITE, Spring Batch rollback le chunk entier
     * et rejoue les 10 items un par un pour isoler le(s) item(s) fautif(s).
     *
     * <h2>Tolérance aux pannes ({@code faultTolerant})</h2>
     *
     * <h3>Skip — Ignorer les fichiers défaillants</h3>
     * <ul>
     *   <li>{@code skipLimit(10)} : autorise jusqu'à 10 fichiers à être ignorés
     *       sur l'ensemble du step. Au-delà du 11ème skip, le step passe en {@code FAILED}.</li>
     *   <li>{@code skip(Exception.class)} : toute exception non fatale déclenche
     *       l'ignorance de l'item courant et l'incrémentation du compteur de skips.</li>
     *   <li>{@code noSkip(OutOfMemoryError.class)} : les erreurs critiques JVM ne sont
     *       jamais ignorées et provoquent l'arrêt immédiat du step.</li>
     * </ul>
     *
     * <h3>Retry — Rejouer en cas d'erreur transitoire</h3>
     * <ul>
     *   <li>{@code retryLimit(3)} : Spring Batch retente le traitement d'un item
     *       jusqu'à 3 fois avant de le considérer comme en échec.</li>
     *   <li>{@code retry(IOException.class)} : seules les {@link java.io.IOException}
     *       déclenchent un retry (ex : fichier temporairement verrouillé, disque lent).
     *       Après 3 échecs consécutifs sur le même item, le skip est appliqué si possible,
     *       sinon le step échoue.</li>
     * </ul>
     *
     * <h2>Observabilité — {@code StepExecutionListener}</h2>
     * Un listener est attaché au step pour assurer la traçabilité :
     * <ul>
     *   <li>{@code beforeStep} : logue le démarrage du step avec l'identifiant
     *       du JobExecution parent, permettant de corréler les logs avec le job.</li>
     *   <li>{@code afterStep} : logue un résumé complet à la fin du step :
     *     <ul>
     *       <li>{@code readCount} : nombre total de fichiers lus par le reader</li>
     *       <li>{@code writeCount} : nombre total de FileRecord écrits avec succès</li>
     *       <li>{@code skipCount} : nombre de fichiers ignorés lors de la lecture ou de l'écriture</li>
     *       <li>{@code processSkipCount} : nombre de fichiers ignorés lors du processing</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h2>Gestion des transactions</h2>
     * Le {@link org.springframework.transaction.PlatformTransactionManager} injecté
     * garantit que chaque chunk est traité dans une transaction distincte.
     * Un rollback ne remet pas en cause les chunks précédents déjà commités.
     *
     * <h2>Exemple de flux nominal</h2>
     * <pre>
     *  Répertoire : /tmp/batch-input/  →  30 fichiers présents
     *
     *  Chunk 1 : lit fichiers  1-10  → traite → écrit → COMMIT
     *  Chunk 2 : lit fichiers 11-20  → traite → écrit → COMMIT
     *  Chunk 3 : lit fichiers 21-30  → traite → écrit → COMMIT
     *
     *  scanStep → ExitStatus.COMPLETED
     *  archiveStep démarre
     * </pre>
     *
     * <h2>Exemple de flux avec erreur</h2>
     * <pre>
     *  Chunk 2 : fichier 15 → IOException → retry 1 → retry 2 → retry 3 → SKIP
     *  skipCount = 1, writeCount = 9 sur ce chunk
     *  Le job continue, archiveStep s'exécute normalement
     * </pre>
     *
     * @param jobRepository le référentiel Spring Batch qui persiste l'état
     *                      d'exécution du step (table {@code BATCH_STEP_EXECUTION})
     * @param txManager     le gestionnaire de transactions utilisé pour délimiter
     *                      chaque chunk dans une transaction atomique
     * @return une instance configurée et prête à l'emploi du {@link org.springframework.batch.core.step.Step}
     *
     * @see FileItemReader
     * @see FileItemProcessor
     * @see FileItemWriter
     * @see org.springframework.batch.core.step.builder.StepBuilder
     * @see org.springframework.batch.core.step.builder.FaultTolerantStepBuilder
     */
    @Bean
    public Step scanStep(JobRepository jobRepository,
                         PlatformTransactionManager txManager) {
        return new StepBuilder("scanStep", jobRepository)
                .<File, FileRecord>chunk(10, txManager)
                .reader(fileItemReader())
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .noSkip(OutOfMemoryError.class)
                .retryLimit(3)
                .retry(IOException.class)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("Démarrage scanStep - Job: {}", stepExecution.getJobExecutionId());
                    }
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("Fin scanStep - lu: {}, écrit: {}, ignoré: {}, erreurs: {}",
                                stepExecution.getReadCount(),
                                stepExecution.getWriteCount(),
                                stepExecution.getSkipCount(),
                                stepExecution.getProcessSkipCount());
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    public Step archiveStep(JobRepository jobRepository,
                            PlatformTransactionManager txManager) {
        return new StepBuilder("archiveStep", jobRepository)
                .tasklet(archiveFileTasklet, txManager)
                .build();
    }

    @Bean
    @StepScope
    public FileItemReader fileItemReader() {
        log.info("Répertoire batch : {}", inputDirectory);
        return new FileItemReader(inputDirectory);
    }
}