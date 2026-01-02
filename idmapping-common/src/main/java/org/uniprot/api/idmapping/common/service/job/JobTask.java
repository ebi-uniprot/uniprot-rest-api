package org.uniprot.api.idmapping.common.service.job;

import static org.uniprot.store.search.SolrCollection.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.search.SolrCollection;

import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Slf4j
public abstract class JobTask implements Runnable {
    private final IdMappingJob job;
    private final IdMappingJobCacheService cacheService;
    public static final Map<SolrCollection, String> COLLECTION_ID_MAP;
    public static final Map<String, String> FROM_SEARCH_FIELD_MAP;

    static {
        COLLECTION_ID_MAP =
                Map.of(
                        uniprot, "accession_id",
                        uniparc, "upi",
                        uniref, "id");
        FROM_SEARCH_FIELD_MAP =
                Map.of(
                        IdMappingFieldConfig.PROTEOME_STR, "proteome",
                        IdMappingFieldConfig.UPARC_STR, "upi",
                        IdMappingFieldConfig.ACC_ID_STR, "accession_id",
                        IdMappingFieldConfig.ACC_STR, "accession_id",
                        IdMappingFieldConfig.SWISSPROT_STR, "accession_id",
                        IdMappingFieldConfig.UNIREF50_STR, "id",
                        IdMappingFieldConfig.UNIREF_90_STR, "id",
                        IdMappingFieldConfig.UNIREF_100_STR, "id");
    }

    public JobTask(IdMappingJob job, IdMappingJobCacheService cacheService) {
        this.job = job;
        this.cacheService = cacheService;
    }

    @Override
    public void run() {
        this.job.setJobStatus(JobStatus.RUNNING);
        this.job.setUpdated(new Date());

        this.cacheService.put(this.job.getJobId(), this.job);
        Stopwatch stopwatch = Stopwatch.createStarted();
        IdMappingResult result = processTask(job);
        stopwatch.stop();

        log.info(
                "[idmapping/run/{}] response took {} seconds (id count={})",
                job.getJobId(),
                stopwatch.elapsed(TimeUnit.SECONDS),
                job.getIdMappingRequest().getIds().split(",").length);

        if (Utils.notNullNotEmpty(result.getErrors())) { //  app constraint violation limit exceed
            populateError(result.getErrors());
            this.job.setIdMappingResult(result);
        } else { // no app constraint violation
            this.job.setJobStatus(JobStatus.FINISHED);
            this.job.setIdMappingResult(result);
            this.job.setUpdated(new Date());
        }
        this.cacheService.put(this.job.getJobId(), this.job);
    }

    protected abstract IdMappingResult processTask(IdMappingJob job);

    private void populateError(List<ProblemPair> errors) {
        this.job.setErrors(errors);
        this.job.setJobStatus(JobStatus.ERROR);
        this.job.setUpdated(new Date());
    }
}
