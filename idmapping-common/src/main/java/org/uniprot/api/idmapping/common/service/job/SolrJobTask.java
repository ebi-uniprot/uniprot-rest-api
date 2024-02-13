package org.uniprot.api.idmapping.common.service.job;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.impl.IdMappingJobServiceImpl;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.store.search.SolrCollection;

public class SolrJobTask extends JobTask {
    private final IdMappingRepository repo;

    public SolrJobTask(
            IdMappingJob job, IdMappingJobCacheService cacheService, IdMappingRepository repo) {
        super(job, cacheService);
        this.repo = repo;
    }

    @Override
    protected IdMappingResult processTask(IdMappingJob job) {
        String toDB = job.getIdMappingRequest().getTo();
        var inputJobIds =
                Arrays.stream(job.getIdMappingRequest().getIds().split(","))
                        .collect(Collectors.toList());

        if (IdMappingJobServiceImpl.UNIREF_SET.contains(toDB)) {
            return queryCollectionAndMapResults(SolrCollection.uniref, inputJobIds);
        } else if (IdMappingJobServiceImpl.UNIPARC.equals(toDB)) {
            return queryCollectionAndMapResults(SolrCollection.uniparc, inputJobIds);
        } else if (IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB)) {
            return queryCollectionAndMapResults(SolrCollection.uniprot, inputJobIds);
        }

        return IdMappingResult.builder()
                .error(
                        new ProblemPair(
                                PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                "unsupported collection"))
                .build();
    }

    private IdMappingResult queryCollectionAndMapResults(
            SolrCollection collection, List<String> inputJobIds) {
        List<IdMappingStringPair> mappedIdsFromSolr;
        try {
            mappedIdsFromSolr = repo.getAllMappingIds(collection, inputJobIds);
        } catch (SolrServerException | IOException e) {
            return IdMappingResult.builder()
                    .error(
                            new ProblemPair(
                                    PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                    "Mapping request got failed"))
                    .build();
        }

        var idMappingResultBuilder = IdMappingResult.builder().mappedIds(mappedIdsFromSolr);
        if (someInputIdsNotFoundInSolr(mappedIdsFromSolr, inputJobIds)) {
            for (String id : inputJobIds) {
                if (idNotExistsInFromDB(mappedIdsFromSolr, id)) {
                    idMappingResultBuilder.unmappedId(id);
                }
            }
        }
        return idMappingResultBuilder.build();
    }

    private boolean someInputIdsNotFoundInSolr(
            List<IdMappingStringPair> mappedIdsFromSolr, List<String> inputJobIds) {
        return mappedIdsFromSolr.size() != inputJobIds.size();
    }

    private boolean idNotExistsInFromDB(List<IdMappingStringPair> mappedIdsFromSolr, String id) {
        return mappedIdsFromSolr.stream()
                .filter(ret -> ret.getFrom().equals(id))
                .findAny()
                .isEmpty();
    }
}
