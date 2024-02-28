package org.uniprot.api.idmapping.common.service;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;

class IdMappingPIRServiceTest {
    private static FakeIdMappingPIRService pirService;

    @BeforeAll
    static void setUp() {
        pirService = new FakeIdMappingPIRService(5);
    }

    @Test
    void queryPageSuccessfully() {
        // given
        IdMappingPageRequest pageRequest = new IdMappingPageRequest();
        int pageSize = 3;
        pageRequest.setSize(pageSize);

        List<IdMappingStringPair> mappingPairs = createMappingPairs(10);
        List<String> unmappedIds = createUnmappedIds(5);
        List<IdMappingStringPair> suggestedIds = createSuggestedIds(2);
        IdMappingResult mappingResult =
                IdMappingResult.builder()
                        .mappedIds(mappingPairs)
                        .unmappedIds(unmappedIds)
                        .suggestedIds(suggestedIds)
                        .obsoleteCount(10)
                        .build();

        // when
        QueryResult<IdMappingStringPair> queryResult =
                pirService.queryResultPage(pageRequest, mappingResult);

        // then
        assertThat(
                queryResult.getContent().collect(Collectors.toList()),
                is(mappingPairs.subList(0, pageSize)));
        assertThat(queryResult.getExtraOptions(), is(notNullValue()));
        ExtraOptions extraOptions = queryResult.getExtraOptions();
        assertThat(extraOptions.getFailedIds(), is(unmappedIds));
        assertThat(extraOptions.getSuggestedIds(), is(suggestedIds));
        assertThat(extraOptions.getObsoleteCount(), is(10));
    }

    @Test
    void queryEmptyPageSuccessfully() {
        // given
        IdMappingPageRequest pageRequest = new IdMappingPageRequest();
        int pageSize = 3;
        pageRequest.setSize(pageSize);
        IdMappingResult mappingResult = IdMappingResult.builder().build();

        // when
        QueryResult<IdMappingStringPair> queryResult =
                pirService.queryResultPage(pageRequest, mappingResult);
        List<Object> emptyList = List.of();
        // then
        assertThat(queryResult.getContent().collect(Collectors.toList()), is(emptyList));
        assertThat(queryResult.getExtraOptions(), is(notNullValue()));
        ExtraOptions extraOptions = queryResult.getExtraOptions();
        assertThat(extraOptions.getFailedIds(), is(emptyList));
        assertThat(extraOptions.getSuggestedIds(), is(emptyList));
        assertThat(extraOptions.getObsoleteCount(), is(nullValue()));
    }

    @Test
    void queryAllResultsSuccessfully() { // given
        List<IdMappingStringPair> mappingPairs = createMappingPairs(10);
        List<String> unmappedIds = createUnmappedIds(5);
        List<IdMappingStringPair> suggestedIds = createSuggestedIds(2);
        IdMappingResult mappingResult =
                IdMappingResult.builder()
                        .mappedIds(mappingPairs)
                        .unmappedIds(unmappedIds)
                        .suggestedIds(suggestedIds)
                        .obsoleteCount(3)
                        .build();

        // when
        QueryResult<IdMappingStringPair> queryResult = pirService.queryResultAll(mappingResult);

        // then
        assertThat(queryResult.getContent().collect(Collectors.toList()), is(mappingPairs));
        assertThat(queryResult.getExtraOptions(), is(notNullValue()));
        ExtraOptions extraOptions = queryResult.getExtraOptions();
        assertThat(extraOptions.getFailedIds(), is(unmappedIds));
        assertThat(extraOptions.getSuggestedIds(), is(suggestedIds));
        assertThat(extraOptions.getObsoleteCount(), is(3));
    }

    private List<IdMappingStringPair> createMappingPairs(int count) {
        return IntStream.range(1, count)
                .mapToObj(i -> new IdMappingStringPair("from " + i, "to " + i))
                .collect(Collectors.toList());
    }

    private List<String> createUnmappedIds(int count) {
        return IntStream.range(1, count)
                .mapToObj(i -> "unmapped " + i)
                .collect(Collectors.toList());
    }

    private List<IdMappingStringPair> createSuggestedIds(int count) {
        return IntStream.range(1, count)
                .mapToObj(i -> new IdMappingStringPair("fromSuggest " + i, "toSuggest " + i))
                .collect(Collectors.toList());
    }

    static class FakeIdMappingPIRService extends IdMappingPIRService {
        public FakeIdMappingPIRService(int defaultPageSize) {
            super(defaultPageSize);
        }

        @Override
        public IdMappingResult mapIds(IdMappingJobRequest request, String jobId) {
            return null;
        }
    }
}
