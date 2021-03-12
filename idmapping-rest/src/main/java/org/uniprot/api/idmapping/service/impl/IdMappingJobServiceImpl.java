package org.uniprot.api.idmapping.service.impl;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import lombok.extern.slf4j.Slf4j;

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
import org.uniprot.api.idmapping.service.HashGenerator;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingJobService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.idmapping.service.job.JobTask;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

import com.google.common.base.Preconditions;

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
        Map<String, String> dbDetails =
                IdMappingFieldConfig.getAllIdMappingTypes().stream()
                        .collect(
                                Collectors.toMap(
                                        UniProtDatabaseDetail::getDisplayName,
                                        UniProtDatabaseDetail::getName));

        UNIREF_SET =
                Stream.of("UniRef50", "UniRef90", "UniRef100")
                        .map(dbDetails::get)
                        .collect(Collectors.toSet());
        UNIPARC = dbDetails.get("UniParc");
        UNIPROTKB_SET =
                Stream.of("UniProtKB AC/ID", "UniProtKB", "UniProtKB Swiss-Prot")
                        .map(dbDetails::get)
                        .collect(Collectors.toSet());

        Preconditions.checkState(
                UNIREF_SET.size() == 3,
                "Expected to extract 3 UniRef database types from: "
                        + IdMappingFieldConfig.class.getName());
        Preconditions.checkNotNull(
                UNIPARC,
                "Expected to extract UniParc database type from: "
                        + IdMappingFieldConfig.class.getName());
        Preconditions.checkState(
                UNIPROTKB_SET.size() == 3,
                "Expected to extract 3 UniProtKB database types from: "
                        + IdMappingFieldConfig.class.getName());
    }

    private final IdMappingJobCacheService cacheService;
    private final IdMappingPIRService pirService;
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final HashGenerator hashGenerator;
    private final String contextPath;

    public IdMappingJobServiceImpl(
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService,
            ThreadPoolTaskExecutor jobTaskExecutor,
            ServletContext servletContext) {
        this.cacheService = cacheService;
        this.pirService = pirService;
        this.jobTaskExecutor = jobTaskExecutor;
        this.hashGenerator = new HashGenerator();
        this.contextPath = servletContext.getContextPath();
    }

    @Override
    public JobSubmitResponse submitJob(IdMappingJobRequest request) {
        String jobId = null;
        jobId = this.hashGenerator.generateHash(request);

        IdMappingJob idMappingJob = createJob(jobId, request);

        if (!this.cacheService.exists(jobId)) {
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
    public String getRedirectPathToResults(IdMappingJob job) {
        String toDB = job.getIdMappingRequest().getTo();
        String dbType = "";
        if (UNIREF_SET.contains(toDB)) {
            dbType = UniRefIdMappingResultsController.UNIREF_ID_MAPPING_PATH + "/";
        } else if (UNIPARC.equals(toDB)) {
            dbType = UniParcIdMappingResultsController.UNIPARC_ID_MAPPING_PATH + "/";
        } else if (UNIPROTKB_SET.contains(toDB)) {
            dbType = UniProtKBIdMappingResultsController.UNIPROTKB_ID_MAPPING_PATH + "/";
        }

        return contextPath
                + IdMappingJobController.IDMAPPING_PATH
                + "/"
                + dbType
                + RESULTS_SUBPATH
                + job.getJobId();
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
}
