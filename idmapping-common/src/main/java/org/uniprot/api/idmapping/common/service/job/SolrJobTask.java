package org.uniprot.api.idmapping.common.service.job;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrServerException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.IdMappingRepository;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.PIRResponseConverter;
import org.uniprot.api.idmapping.common.service.impl.IdMappingJobServiceImpl;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.store.search.SolrCollection;

public class SolrJobTask extends JobTask {
    private final IdMappingRepository repo;
    private final PIRResponseConverter responseConverter;
    private final Integer maxIdMappingToIdsCountEnriched;
    private final Integer maxIdMappingToIdsCount;

    public SolrJobTask(
            IdMappingJob job,
            IdMappingJobCacheService cacheService,
            IdMappingRepository repo,
            Integer maxIdMappingToIdsCountEnriched,
            Integer maxIdMappingToIdsCount) {
        super(job, cacheService);
        this.repo = repo;
        this.responseConverter = new PIRResponseConverter();
        this.maxIdMappingToIdsCountEnriched = maxIdMappingToIdsCountEnriched;
        this.maxIdMappingToIdsCount = maxIdMappingToIdsCount;
    }

    @Override
    protected IdMappingResult processTask(IdMappingJob job) {
        String toDB = job.getIdMappingRequest().getTo();
        String fromDB = job.getIdMappingRequest().getFrom();
        IdMappingJobRequest request = job.getIdMappingRequest();
        var inputJobIds =
                Arrays.stream(job.getIdMappingRequest().getIds().split(","))
                        .collect(Collectors.toList());
        String fromSearchField = FROM_SEARCH_FIELD_MAP.get(fromDB);
        if (IdMappingJobServiceImpl.PROTEOME.equals(fromDB)) {
            List<String> proteomeIds = inputJobIds.stream().map(String::toLowerCase).toList();
            if (IdMappingJobServiceImpl.UNIPARC.equals(toDB)) {
                return queryCollectionAndMapResults(
                        request,
                        SolrCollection.uniparc,
                        proteomeIds,
                        fromSearchField,
                        COLLECTION_ID_MAP.get(SolrCollection.uniparc));
            } else if (IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB)) {
                return queryCollectionAndMapResults(
                        request,
                        SolrCollection.uniprot,
                        proteomeIds,
                        fromSearchField,
                        COLLECTION_ID_MAP.get(SolrCollection.uniprot));
            }
        } else {
            if (IdMappingJobServiceImpl.UNIREF_SET.contains(toDB)) {
                return queryCollectionAndMapResults(
                        request,
                        SolrCollection.uniref,
                        inputJobIds,
                        fromSearchField,
                        COLLECTION_ID_MAP.get(SolrCollection.uniref));
            } else if (IdMappingJobServiceImpl.UNIPARC.equals(toDB)) {
                return queryCollectionAndMapResults(
                        request,
                        SolrCollection.uniparc,
                        inputJobIds,
                        fromSearchField,
                        COLLECTION_ID_MAP.get(SolrCollection.uniparc));
            } else if (IdMappingJobServiceImpl.UNIPROTKB_SET.contains(toDB)) {
                return queryCollectionAndMapResults(
                        request,
                        SolrCollection.uniprot,
                        inputJobIds,
                        fromSearchField,
                        COLLECTION_ID_MAP.get(SolrCollection.uniprot));
            }
        }

        return IdMappingResult.builder()
                .error(
                        new ProblemPair(
                                PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                "unsupported collection"))
                .build();
    }

    private IdMappingResult queryCollectionAndMapResults(
            IdMappingJobRequest jobRequest,
            SolrCollection collection,
            List<String> inputJobIds,
            String searchField,
            String idField) {
        List<IdMappingStringPair> mappedIdsFromSolr;
        try {
            mappedIdsFromSolr =
                    repo.getAllMappingIds(collection, inputJobIds, searchField, idField);
        } catch (SolrServerException | IOException e) {
            return IdMappingResult.builder()
                    .error(
                            new ProblemPair(
                                    PredefinedAPIStatus.SERVER_ERROR.getCode(),
                                    "Mapping request got failed"))
                    .build();
        }

        var idMappingResultBuilder = IdMappingResult.builder().mappedIds(mappedIdsFromSolr);
        idMappingResultBuilder =
                this.responseConverter.populateErrorOrWarning(
                        jobRequest,
                        idMappingResultBuilder,
                        this.maxIdMappingToIdsCountEnriched,
                        this.maxIdMappingToIdsCount);

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
