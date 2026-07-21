package org.uniprot.api.idmapping.common.service.job;

import static org.uniprot.api.idmapping.common.service.impl.IdMappingJobServiceImpl.*;
import static org.uniprot.store.search.SolrCollection.*;

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
        String fromDB = job.getIdMappingRequest().getFrom();
        String toDB = job.getIdMappingRequest().getTo();
        var inputJobIds =
                Arrays.stream(job.getIdMappingRequest().getIds().split(","))
                        .collect(Collectors.toList());

        if (MD5.equals(fromDB)) {
            List<String> idListLowerCase = inputJobIds.stream().map(String::toLowerCase).toList();
            String query = "UniProtKB".equals(toDB) ? "*:*" : "reviewed:true";
            return queryCollectionAndMapResults(uniprot, idListLowerCase, query, "checksum");
        } else if (UNIREF_SET.contains(toDB)) {
            return queryCollectionAndMapResults(uniref, inputJobIds, null, null);
        } else if (UNIPARC.equals(toDB)) {
            return queryCollectionAndMapResults(uniparc, inputJobIds, null, null);
        } else if (UNIPROTKB_SET.contains(toDB)) {
            return queryCollectionAndMapResults(uniprot, inputJobIds, null, null);
        }

        return IdMappingResult.builder()
                .error(
                        new ProblemPair(
                                PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                "unsupported collection"))
                .build();
    }

    private IdMappingResult queryCollectionAndMapResults(
            SolrCollection collection, List<String> inputJobIds, String query, String searchField) {
        List<IdMappingStringPair> mappedIdsFromSolr;
        try {
            if (query != null && searchField != null) {
                mappedIdsFromSolr =
                        repo.getAllMappingIds(collection, searchField, inputJobIds, query);
            } else if (query != null) {
                mappedIdsFromSolr = repo.getAllMappingIds(collection, inputJobIds, query);
            } else if (searchField != null) {
                mappedIdsFromSolr = repo.getAllMappingIds(collection, searchField, inputJobIds);
            } else {
                mappedIdsFromSolr = repo.getAllMappingIds(collection, inputJobIds);
            }

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
