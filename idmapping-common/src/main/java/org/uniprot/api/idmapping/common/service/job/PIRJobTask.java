package org.uniprot.api.idmapping.common.service.job;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingPIRService;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PIRJobTask extends JobTask {
    private static final int BATCH_SIZE = 100_000;
    private final IdMappingPIRService pirService;

    private final IdMappingRepository idMappingRepository;

    public PIRJobTask(
            IdMappingJob job,
            IdMappingJobCacheService cacheService,
            IdMappingPIRService pirService,
            IdMappingRepository idMappingRepository) {
        super(job, cacheService);
        this.pirService = pirService;
        this.idMappingRepository = idMappingRepository;
    }

    @Override
    protected IdMappingResult processTask(IdMappingJob job) {
        try {
            IdMappingResult result = pirService.mapIds(job.getIdMappingRequest(), job.getJobId());
            // set obsolete count if needed
            if (Objects.nonNull(result)
                    && Utils.nullOrEmpty(result.getErrors())
                    && Objects.nonNull(job.getIdMappingRequest())
                    && IdMappingFieldConfig.UNIPROTKB_STR.equals(
                            job.getIdMappingRequest().getTo())) {

                Integer obsoleteUniProtCount = getObsoleteUniProtEntryCount(result, BATCH_SIZE);
                result.setObsoleteCount(obsoleteUniProtCount);
            }
            return result;
        } catch (RestClientException restException) {
            return IdMappingResult.builder()
                    .error(
                            new ProblemPair(
                                    PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                    restException.getMessage()))
                    .build();
        } catch (Exception ex) {
            log.error(
                    "Error while processing PIR response for jobId {} and the error is {}",
                    job.getJobId(),
                    ex.getCause());
            return IdMappingResult.builder()
                    .error(
                            new ProblemPair(
                                    PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                    "Internal server error."))
                    .build();
        }
    }

    Integer getObsoleteUniProtEntryCount(IdMappingResult result, int batchSize)
            throws SolrServerException, IOException {
        List<String> accessions =
                result.getMappedIds().stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toList());
        Set<IdMappingStringPair> obsoleteIdPairs = new HashSet<>();
        for (int i = 0; i < accessions.size(); i += batchSize) {
            List<String> batch = accessions.subList(i, Math.min(i + batchSize, accessions.size()));
            obsoleteIdPairs.addAll(
                    this.idMappingRepository.getAllMappingIds(
                            SolrCollection.uniprot,
                            batch,
                            "active:false",
                            "accession_id",
                            COLLECTION_ID_MAP.get(SolrCollection.uniprot)));
        }

        return obsoleteIdPairs.size();
    }
}
