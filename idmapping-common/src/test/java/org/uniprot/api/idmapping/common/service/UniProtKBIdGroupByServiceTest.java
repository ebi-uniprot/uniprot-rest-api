package org.uniprot.api.idmapping.common.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdGroupByRequest;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;

public abstract class UniProtKBIdGroupByServiceTest<T> {
    protected static final String QUERY = "query";
    protected static final String FACET_NAME = "facetName";
    protected static final String GROUP_0 = "group0";
    protected static final int COUNT_0 = 12;
    protected static final String PARENT = "parent";
    protected static final String GROUP_1 = "group1";
    protected static final int COUNT_1 = 99;
    protected static final String GROUP_2 = "group2";
    protected static final int COUNT_2 = 500;
    protected GroupByService<T> groupByService;
    @Mock protected UniProtEntryService uniProtEntryService;
    @Mock private UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig;
    protected UniProtKBIdGroupByService<T> idGroupByService;
    @Mock private UniProtKBIdGroupByRequest groupByRequest;
    @Mock private Map<String, String> facetParams0;
    @Mock private Map<String, String> facetParams1;
    private final List<FacetField> facetFieldsSingle =
            List.of(
                    new FacetField(FACET_NAME) {
                        {
                            add(GROUP_0, COUNT_0);
                        }
                    });
    private final List<FacetField> facetFieldsMultiple =
            List.of(
                    new FacetField(FACET_NAME) {
                        {
                            add(GROUP_1, COUNT_1);
                        }
                    },
                    new FacetField(FACET_NAME) {
                        {
                            add(GROUP_2, COUNT_2);
                        }
                    });
    @Mock private SearchFieldConfig searchFieldConfig;
    @Mock private GroupByResult groupByResult0;
    @Mock private GroupByResult groupByResult1;

    protected void init() {
        when(uniProtEntryService.getQueryProcessorConfig())
                .thenReturn(uniProtKBQueryProcessorConfig);
        when(uniProtKBQueryProcessorConfig.getSearchFieldConfig()).thenReturn(searchFieldConfig);
        when(groupByRequest.getIds()).thenReturn(List.of("id0", "id1", "id2"));
    }

    @Test
    void getGroupByResult_whenParentNotSpecified() {
        prepareForEmptyParent();
        when(groupByService.getFacetParams(getInitialEntries())).thenReturn(facetParams0);
        when(groupByRequest.getQuery()).thenReturn(QUERY);
        when(uniProtEntryService.getFacets(eq(QUERY), eq(facetParams0), anyList()))
                .thenReturn(facetFieldsMultiple);
        when(groupByService.getGroupByResult(
                        anyList(), anyList(), anyList(), eq(null), anyList(), eq(QUERY)))
                .thenReturn(groupByResult0);

        GroupByResult test = idGroupByService.getGroupByResult(groupByRequest);

        assertSame(groupByResult0, test);
    }

    @Test
    void getGroupByResult_whenParentSpecified() {
        prepareWithParent();
        when(groupByService.getFacetParams(getInitialEntries())).thenReturn(facetParams0);
        when(groupByRequest.getQuery()).thenReturn(QUERY);
        when(groupByRequest.getParent()).thenReturn(UniProtKBIdGroupByServiceTest.PARENT);
        when(uniProtEntryService.getFacets(eq(QUERY), eq(facetParams0), anyList()))
                .thenReturn(facetFieldsMultiple);
        when(groupByService.getGroupByResult(
                        anyList(), anyList(), anyList(), eq(PARENT), anyList(), eq(QUERY)))
                .thenReturn(groupByResult0);

        GroupByResult test = idGroupByService.getGroupByResult(groupByRequest);

        assertSame(groupByResult0, test);
    }

    @Test
    void getGroupByResult_whenParentSpecifiedAndSingleFacetsWithNoChildren() {
        prepareWithParent();
        when(groupByService.getFacetParams(getInitialEntries())).thenReturn(facetParams0);
        when(groupByRequest.getQuery()).thenReturn(QUERY);
        when(groupByRequest.getParent()).thenReturn(UniProtKBIdGroupByServiceTest.PARENT);
        when(uniProtEntryService.getFacets(eq(QUERY), eq(facetParams0), anyList()))
                .thenReturn(facetFieldsSingle);
        when(groupByService.getGroupByResult(
                        anyList(), anyList(), anyList(), eq(PARENT), anyList(), eq(QUERY)))
                .thenReturn(groupByResult0);

        GroupByResult test = idGroupByService.getGroupByResult(groupByRequest);

        assertSame(groupByResult0, test);
    }

    @Test
    void getGroupByResult_whenParentSpecifiedAndSingleFacetsWithChildren() {
        prepareWithParent();
        when(groupByService.getFacetParams(getInitialEntries())).thenReturn(facetParams0);
        lenient().when(groupByService.getFacetParams(getChildEntries())).thenReturn(facetParams1);
        when(groupByRequest.getQuery()).thenReturn(QUERY);
        when(groupByRequest.getParent()).thenReturn(UniProtKBIdGroupByServiceTest.PARENT);
        when(uniProtEntryService.getFacets(eq(QUERY), eq(facetParams0), anyList()))
                .thenReturn(facetFieldsSingle);
        lenient()
                .when(uniProtEntryService.getFacets(eq(QUERY), eq(facetParams1), anyList()))
                .thenReturn(facetFieldsMultiple);
        when(groupByService.getGroupByResult(
                        anyList(), anyList(), anyList(), eq(PARENT), anyList(), eq(QUERY)))
                .thenReturn(groupByResult1);
        lenient().when(groupByService.getChildEntries(GROUP_0)).thenReturn(getChildEntries());

        GroupByResult test = idGroupByService.getGroupByResult(groupByRequest);

        assertSame(groupByResult1, test);
    }

    protected abstract void prepareForEmptyParent();

    protected abstract void prepareWithParent();

    protected abstract List<T> getInitialEntries();

    protected abstract List<T> getChildEntries();
}
