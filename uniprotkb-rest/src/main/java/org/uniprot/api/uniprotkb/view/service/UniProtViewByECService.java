package org.uniprot.api.uniprotkb.view.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.cv.ec.ECRepo;

import com.google.common.base.Strings;

// @Service
public class UniProtViewByECService implements UniProtViewByService {
    private final SolrClient solrClient;
    private final String uniprotCollection;
    private final ECRepo ecRepo;
    public static final String URL_PREFIX = "https://enzyme.expasy.org/EC/";

    public UniProtViewByECService(SolrClient solrClient, String uniprotCollection, ECRepo ecRepo) {
        this.solrClient = solrClient;
        this.uniprotCollection = uniprotCollection;
        this.ecRepo = ecRepo;
    }

    public List<ViewBy> get(String queryStr, String parent) {
        SolrQuery query = new SolrQuery(queryStr);
        StringBuilder regEx = new StringBuilder();
        String regExPostfix = "[0-9]+";
        String parentEc = parent;
        if (!Strings.isNullOrEmpty(parent)) {
            parentEc = ecRemoveDash(parentEc);
            String[] tokens = parentEc.split("\\.");
            for (String token : tokens) {
                regEx.append(token).append("\\.");
            }
        }
        regEx.append(regExPostfix);
        query.setFacet(true);
        query.add(FacetParams.FACET_FIELD, "ec");
        query.add(FacetParams.FACET_MATCHES, regEx.toString());
        query.add(FacetParams.FACET_MINCOUNT, "1");
        try {
            QueryResponse response = solrClient.query(uniprotCollection, query);
            List<FacetField> fflist = response.getFacetFields();
            if (fflist.isEmpty()) {
                return Collections.emptyList();
            } else {
                FacetField ff = fflist.get(0);
                List<FacetField.Count> counts = ff.getValues();
                return counts.stream()
                        // .filter(val -> val.getCount()>0)
                        .map(this::convert)
                        .sorted(ViewBy.SORT_BY_ID)
                        .collect(Collectors.toList());
            }
        } catch (SolrServerException | IOException e) {
            throw new UniProtViewByServiceException(e);
        }
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

    private String ecRemoveDash(String ec) {
        String temp = ec;
        while (temp.endsWith(".-")) {
            temp = temp.substring(0, temp.length() - 2);
        }
        return temp;
    }

    private ViewBy convert(FacetField.Count count) {
        ViewBy viewBy = new ViewBy();
        String ecId = count.getName();
        String fullEc = ecAddDashIfAbsent(ecId);
        if (fullEc.contains(".-")) {
            viewBy.setExpand(true);
        } else {
            viewBy.setExpand(false);
        }
        Optional<ECEntry> ecOpt = ecRepo.getEC(fullEc);
        viewBy.setId(fullEc);
        ecOpt.ifPresent(ec -> viewBy.setLabel(ec.label()));

        viewBy.setLink(URL_PREFIX + fullEc);
        viewBy.setCount(count.getCount());
        return viewBy;
    }
}
