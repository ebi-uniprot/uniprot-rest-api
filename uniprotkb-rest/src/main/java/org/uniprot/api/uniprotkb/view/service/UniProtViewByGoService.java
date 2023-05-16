package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.uniprot.api.uniprotkb.view.GoRelation;
import org.uniprot.api.uniprotkb.view.GoTerm;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByGoService extends UniProtViewByService<GoRelation> {
    public static final String URL_PREFIX = "https://www.ebi.ac.uk/QuickGO/term/";
    private static final String GO_PREFIX = "GO:";
    private final GoService goService;

    public UniProtViewByGoService(SolrClient solrClient, String uniprotCollection, GoService goService) {
        super(solrClient, uniprotCollection);
        this.goService = goService;
    }

    @Override
    List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<GoRelation> entries, String queryStr) {
        Map<String, GoRelation> goRelationMap = entries.stream()
                .collect(Collectors.toMap(GoRelation::getId, Function.identity()));
        return facetCounts.stream()
                .map(fc -> createViewBy(fc, goRelationMap.get(fc.getName())))
                .sorted(ViewBy.SORT_BY_LABEL)
                .collect(Collectors.toList());
    }

    private ViewBy createViewBy(FacetField.Count count, GoRelation goRelation) {
        ViewBy viewBy = new ViewBy();
        String goId = addGoPrefix(count.getName());
        viewBy.setId(goId);
        viewBy.setCount(count.getCount());
        viewBy.setLink(URL_PREFIX + goId);
        viewBy.setLabel(goRelation.getName());
        viewBy.setExpand(goRelation.isHasChildren());
        return viewBy;
    }

    private String addGoPrefix(String go) {
        if (!isOpenParentSearch(go) && !go.startsWith(GO_PREFIX)) {
            return GO_PREFIX + go;
        }
        return go;
    }

    @Override
    SolrQuery getSolrQuery(String queryStr, List<GoRelation> entries) {
        SolrQuery query = new SolrQuery(queryStr);
        String facetItems = entries.stream()
                .map(GoRelation::getId)
                .map(this::removeGoPrefix)
                .collect(Collectors.joining(","));
        String facetField = "{!terms='" + facetItems + "'}go_id";
        query.setFacet(true);
        query.addFacetField(facetField);
        return query;
    }

    private String removeGoPrefix(String go) {
        if (go != null && go.startsWith(GO_PREFIX)) {
            return go.substring(GO_PREFIX.length());
        }
        return go;
    }

    @Override
    List<GoRelation> getChildren(String parent) {
        String parentGo = addGoPrefix(parent);
        return goService.getChildren(parentGo).map(GoTerm::getChildren).orElse(Collections.emptyList());
    }
}
