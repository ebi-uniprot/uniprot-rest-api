package org.uniprot.api.uniprotkb.view.service;

import com.google.common.base.Strings;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.GoRelation;
import org.uniprot.api.uniprotkb.view.GoTerm;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UniProtViewByGoService extends UniProtViewByService<GoRelation> {
    private final SolrClient solrClient;
    private final String uniprotCollection;
    private final GoService goService;
    public static final String URL_PREFIX = "https://www.ebi.ac.uk/QuickGO/term/";
    private static final String GO_PREFIX = "GO:";

    public UniProtViewByGoService(
            SolrClient solrClient, String uniprotCollection, GoService goService) {
        super(null);
        this.solrClient = solrClient;
        this.uniprotCollection = uniprotCollection;
        this.goService = goService;
    }

    @Override
    public List<ViewBy> getViewBys(String queryStr, String parent) {
        String parentGo = addGoPrefix(parent);
        Optional<GoTerm> goTerm = goService.getChildren(parentGo);
        if (!goTerm.isPresent()) {
            return Collections.emptyList();
        }
        List<GoRelation> children = goTerm.get().getChildren();
        if (children.isEmpty()) return Collections.emptyList();
        Map<String, GoRelation> gorelationMap =
                children.stream().collect(Collectors.toMap(GoRelation::getId, Function.identity()));

        String facetIterms =
                children.stream()
                        .map(GoRelation::getId)
                        .map(this::removeGoPrefix)
                        .collect(Collectors.joining(","));
        SolrQuery query = new SolrQuery(queryStr);
        String facetField = "{!terms='" + facetIterms + "'}go_id";
        query.setFacet(true);
        query.addFacetField(facetField);

        try {
            QueryResponse response = solrClient.query(uniprotCollection, query);
            List<FacetField> fflist = response.getFacetFields();
            if (fflist.isEmpty()) {
                return Collections.emptyList();
            } else {
                FacetField ff = fflist.get(0);
                List<FacetField.Count> counts = ff.getValues();
                return counts.stream()
                        .map(val -> convert(val, gorelationMap))
                        .filter(val -> val != null)
                        .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
            }
        } catch (SolrServerException | IOException e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    @Override
    protected List<GoRelation> getChildren(String parent) {
        return null;
    }

    @Override
    protected String getFacetFields(List<GoRelation> goRelations) {
        return null;
    }

    @Override
    protected List<ViewBy> getViewBys(List<FacetField.Count> facetCounts, List<GoRelation> goRelations, String query) {
        return null;
    }

    private String removeGoPrefix(String go) {
        if ((go != null) && go.startsWith(GO_PREFIX)) {
            return go.substring(GO_PREFIX.length());
        } else return go;
    }

    private String addGoPrefix(String go) {
        if (!Strings.isNullOrEmpty(go) && !go.startsWith(GO_PREFIX)) {
            return GO_PREFIX + go;
        } else return go;
    }

    private ViewBy convert(FacetField.Count count, Map<String, GoRelation> gorelationMap) {
        String goId = addGoPrefix(count.getName());
        Optional<GoRelation> goRelation = Optional.ofNullable(gorelationMap.get(goId));
        return ViewByImpl.builder()
                .id(goId)
                .count(count.getCount())
                .link(URL_PREFIX + goId)
                .label(goRelation.map(GoRelation::getName).orElse(""))
                .expand(goRelation.map(GoRelation::isHasChildren).orElse(false)).build();
    }
}
