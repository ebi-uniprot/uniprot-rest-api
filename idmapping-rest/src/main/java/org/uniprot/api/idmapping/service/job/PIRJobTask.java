package org.uniprot.api.idmapping.service.job;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.web.client.RestClientException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.repository.IdMappingRepository;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.service.IdMappingPIRService;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;
import org.uniprot.store.search.SolrCollection;

@Slf4j
public class PIRJobTask extends JobTask {
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

                Integer obsoleteUniProtCount = getObsoleteUniProtEntryCount(result);
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

    private Integer getObsoleteUniProtEntryCount(IdMappingResult result)
            throws SolrServerException, IOException {
        List<String> accessions =
                result.getMappedIds().stream()
                        .map(IdMappingStringPair::getTo)
                        .collect(Collectors.toList());
        List<IdMappingStringPair> obsoleteIdPairs =
                this.idMappingRepository.getAllMappingIds(
                        SolrCollection.uniprot, accessions, "active:false");
        return obsoleteIdPairs.size();
    }
}
