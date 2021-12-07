package org.uniprot.api.idmapping.service.impl;

import java.util.Date;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.controller.IdMappingJobController;
import org.uniprot.api.idmapping.controller.UniParcIdMappingResultsController;
import org.uniprot.api.idmapping.controller.UniProtKBIdMappingResultsController;
import org.uniprot.api.idmapping.controller.UniRefIdMappingResultsController;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.response.JobSubmitResponse;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingJobService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.job.JobTask;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * Created 23/02/2021
 *
 * @author sahmad
 */
@Service
@Slf4j
public class IdMappingJobServiceImpl implements IdMappingJobService {
    private static final String RESULTS_SUBPATH = "results/";
    private static final Set<String> UNIREF_SET;
    private static final String UNIPARC;
    private static final Set<String> UNIPROTKB_SET;

    static {
        UNIPARC = IdMappingFieldConfig.UPARC_STR;

        UNIREF_SET =
                Set.of(
                        IdMappingFieldConfig.UNIREF50_STR,
                        IdMappingFieldConfig.UNIREF90_STR,
                        IdMappingFieldConfig.UNIREF100_STR);

        UNIPROTKB_SET =
                Set.of(
                        IdMappingFieldConfig.ACC_ID_STR,
                        IdMappingFieldConfig.ACC_STR,
                        IdMappingFieldConfig.SWISSPROT_STR);
    }

    private final IdMappingJobCacheService cacheService;
    private final IdMappingPIRService pirService;
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final HashGenerator hashGenerator;

    @Value("${id.mapping.max.to.ids.enrich.count:#{null}}") // value to 100k
    private Integer maxIdMappingToIdsCountEnriched;

    public IdMappingJobServiceImpl(
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService,
            ThreadPoolTaskExecutor jobTaskExecutor) {
        this.cacheService = cacheService;
        this.pirService = pirService;
        this.jobTaskExecutor = jobTaskExecutor;
        this.hashGenerator = new HashGenerator();
    }

    @Override
    public JobSubmitResponse submitJob(IdMappingJobRequest request) {
        String jobId = null;
        jobId = this.hashGenerator.generateHash(request);

        IdMappingJob idMappingJob = createJob(jobId, request);

        if (needToRunJob(jobId)) {
            this.cacheService.put(jobId, idMappingJob);
            log.debug(
                    "Put into cache, {} ids: {}...",
                    idMappingJob.getIdMappingRequest().getIds().split(",").length,
                    idsForLog(idMappingJob.getIdMappingRequest().getIds()));
            // create task and submit
            JobTask jobTask = new JobTask(idMappingJob, pirService);
            jobTaskExecutor.execute(jobTask);
        } else {
            IdMappingJob job = this.cacheService.get(jobId);

            // update expiry time
            job.setUpdated(new Date());
        }

        return new JobSubmitResponse(jobId);
    }

    @Override
    public String getRedirectPathToResults(IdMappingJob job, String requestUrl) {
        String toDB = job.getIdMappingRequest().getTo();
        String dbType = "";
        if(isMoreThanAllowedMappedIdsToEnrich(job.getIdMappingResult())){ // just return mapped ids without enrichment
            dbType = "";
        } else if (UNIREF_SET.contains(toDB)) {
            dbType = UniRefIdMappingResultsController.UNIREF_ID_MAPPING_PATH + "/";
        } else if (UNIPARC.equals(toDB)) {
            dbType = UniParcIdMappingResultsController.UNIPARC_ID_MAPPING_PATH + "/";
        } else if (UNIPROTKB_SET.contains(toDB)) {
            dbType = UniProtKBIdMappingResultsController.UNIPROTKB_ID_MAPPING_PATH + "/";
        }

        String requestUrlBase = extractRequestBase(requestUrl);
        return requestUrlBase + "/" + dbType + RESULTS_SUBPATH + job.getJobId();
    }

    private String extractRequestBase(String requestUrl) {
        int endOfIdMappingPath =
                requestUrl.indexOf(IdMappingJobController.IDMAPPING_PATH)
                        + IdMappingJobController.IDMAPPING_PATH.length();
        return requestUrl.substring(0, endOfIdMappingPath).replaceFirst("http://", "https://");
    }

    private boolean needToRunJob(String jobId) {
        boolean exists = this.cacheService.exists(jobId);
        if (exists) {
            IdMappingJob job = this.cacheService.get(jobId);
            if (job.getJobStatus().equals(JobStatus.ERROR)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private String idsForLog(String logIds) {
        if (logIds.length() > 40) {
            return logIds.substring(0, 40);
        } else {
            return logIds;
        }
    }

    private IdMappingJob createJob(String jobId, IdMappingJobRequest request) {
        IdMappingJob.IdMappingJobBuilder builder = IdMappingJob.builder();
        builder.jobId(jobId).jobStatus(JobStatus.NEW);
        builder.idMappingRequest(request);
        return builder.build();
    }

    private boolean isMoreThanAllowedMappedIdsToEnrich(IdMappingResult idMappingResult) {
        return Utils.notNull(idMappingResult) &&
                Utils.notNullNotEmpty(idMappingResult.getMappedIds()) &&
                idMappingResult.getMappedIds().size() > this.maxIdMappingToIdsCountEnriched;
    }
}
