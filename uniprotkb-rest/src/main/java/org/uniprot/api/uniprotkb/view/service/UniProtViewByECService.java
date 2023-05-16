package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// @Service
public class UniProtViewByECService extends UniProtViewByService<String> {
    public static final String URL_PREFIX = "https://enzyme.expasy.org/EC/";
    private final ECRepo ecRepo;

    public UniProtViewByECService(SolrClient solrClient, String uniProtCollection, ECRepo ecRepo) {
        super(solrClient, uniProtCollection);
        this.ecRepo = ecRepo;
    }

    @Override
    List<ViewBy> createViewBys(List<FacetField.Count> facetCounts, List<String> entries, String queryStr) {
        return facetCounts.stream()
                .map(this::createViewBy)
                .sorted(ViewBy.SORT_BY_ID)
                .collect(Collectors.toList());
    }

    private ViewBy createViewBy(FacetField.Count count) {
        ViewBy viewBy = new ViewBy();
        String ecId = count.getName();
        String fullEc = ecAddDashIfAbsent(ecId);
        viewBy.setExpand(fullEc.contains(".-"));
        viewBy.setId(fullEc);
        Optional<ECEntry> ecOpt = ecRepo.getEC(fullEc);
        ecOpt.ifPresent(ec -> viewBy.setLabel(ec.getLabel()));
        viewBy.setLink(URL_PREFIX + fullEc);
        viewBy.setCount(count.getCount());
        return viewBy;
    }

    private String ecAddDashIfAbsent(String ec) {
        String[] tokens = ec.split("\\.");
        if (tokens.length == 4) {
            return ec;
        } else if (tokens.length == 3) {
            return ec + ".-";
        } else if (tokens.length == 2) {
            return ec + ".-.-";
        } else {
            return ec + ".-.-.-";
        }
    }

    @Override
    SolrQuery getSolrQuery(String queryStr, List<String> entries) {
        SolrQuery query = new SolrQuery(queryStr);
        String regEx = entries.stream().map(token -> token + "\\.").collect(Collectors.joining("", "", "[0-9]+"));
        query.add(FacetParams.FACET_MATCHES, regEx);
        query.add(FacetParams.FACET_FIELD, "ec");
        query.add(FacetParams.FACET_MINCOUNT, "1");
        query.setFacet(true);
        return query;
    }

    @Override
    List<String> getChildren(String parent) {
        List<String> children = new LinkedList<>();
        String parentEc = parent;
        if (!isOpenParentSearch(parentEc)) {
            parentEc = ecRemoveDash(parentEc);
            String[] tokens = parentEc.split("\\.");
            children.addAll(Arrays.asList(tokens));
        }
        return children;
    }

    private String ecRemoveDash(String ec) {
        String temp = ec;
        while (temp.endsWith(".-")) {
            temp = temp.substring(0, temp.length() - 2);
        }
        return temp;
    }
}
