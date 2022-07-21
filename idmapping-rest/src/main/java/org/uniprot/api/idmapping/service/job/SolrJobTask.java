package org.uniprot.api.idmapping.service.job;

import static org.uniprot.api.idmapping.service.impl.IdMappingJobServiceImpl.*;
import static org.uniprot.store.search.SolrCollection.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.repository.IdMappingRepository;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.store.search.SolrCollection;

public class SolrJobTask extends JobTask {
    private final IdMappingRepository repo;

    private static final int EXCEPTION_CODE = 50;

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

        if (UNIREF_SET.contains(toDB)) {
            return queryCollectionAndMapResults(uniref, inputJobIds);
        } else if (UNIPARC.equals(toDB)) {
            return queryCollectionAndMapResults(uniparc, inputJobIds);
        } else if (UNIPROTKB_SET.contains(toDB)) {
            return queryCollectionAndMapResults(uniprot, inputJobIds);
        }

        return IdMappingResult.builder()
                .error(new ProblemPair(EXCEPTION_CODE, "unsupported collection"))
                .build();
    }

    private IdMappingResult queryCollectionAndMapResults(
            SolrCollection collection, List<String> inputJobIds) {
        List<IdMappingStringPair> mappedIdsFromSolr;
        try {
            mappedIdsFromSolr = repo.getAllMappingIds(collection, inputJobIds);
        } catch (SolrServerException | IOException e) {
            return IdMappingResult.builder()
                    .error(new ProblemPair(EXCEPTION_CODE, "Mapping request got failed"))
                    .build();
        }

        var idMappingResultBuilder = IdMappingResult.builder().mappedIds(mappedIdsFromSolr);
        if (mappedIdsFromSolr.size() != inputJobIds.size()) {
            inputJobIds.forEach(
                    id -> {
                        if (mappedIdsFromSolr.stream()
                                .filter(ret -> ret.getFrom().equals(id))
                                .findAny()
                                .isEmpty()) {
                            idMappingResultBuilder.unmappedId(id);
                        }
                    });
        }
        return idMappingResultBuilder.build();
    }
}
