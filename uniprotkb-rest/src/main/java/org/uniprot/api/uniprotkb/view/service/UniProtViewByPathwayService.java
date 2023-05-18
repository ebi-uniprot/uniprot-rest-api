package org.uniprot.api.uniprotkb.view.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.FacetParams;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.pathway.UniPathway;
import org.uniprot.cv.pathway.UniPathwayRepo;

import com.google.common.base.Strings;

public class UniProtViewByPathwayService implements UniProtViewByService {
    private final SolrClient solrClient;
    private final String uniprotCollection;
    private final UniPathwayRepo unipathwayRepo;
    // private final static String URL_PREFIX ="https://www.uniprot.org/keywords/";

    public UniProtViewByPathwayService(
            SolrClient solrClient, String uniprotCollection, UniPathwayRepo unipathwayRepo) {
        this.solrClient = solrClient;
        this.uniprotCollection = uniprotCollection;
        this.unipathwayRepo = unipathwayRepo;
    }

    @Override
    public List<ViewBy> getViewBys(String queryStr, String parent) {
        SolrQuery query = new SolrQuery(queryStr);
        StringBuilder regEx = new StringBuilder();
        String regExPostfix = "[0-9\\.]+";
        String parentEc = parent;
        if (!Strings.isNullOrEmpty(parent)) {
            String[] tokens = parentEc.split("\\.");
            for (String token : tokens) {
                regEx.append(token).append("\\.");
            }
        }
        regEx.append(regExPostfix);
        query.setFacet(true);
        query.add(FacetParams.FACET_FIELD, "pathway");
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
                Map<String, FacetField.Count> idCountMap =
                        counts.stream()
                                .collect(
                                        Collectors.toMap(
                                                FacetField.Count::getName, Function.identity()));
                List<UniPathway> pathways = unipathwayRepo.getChildrenById(parent);

                return pathways.stream()
                        .map(val -> getRightLevelPathway(val, idCountMap))
                        .distinct()
                        .map(val -> convert(val, idCountMap))
                        .filter(val -> val != null)
                        .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                        .collect(Collectors.toList());
            }
        } catch (SolrServerException | IOException e) {
            throw new UniProtViewByServiceException(e);
        }
    }

    private ViewBy convert(UniPathway pathway, Map<String, FacetField.Count> idCountMap) {
        FacetField.Count count = idCountMap.get(pathway.getId());
        if (count == null) return null;
        ViewBy viewBy = new ViewBy();
        FacetField.Count validCount = idCountMap.get(pathway.getId());
        String id = validCount.getName();
        viewBy.setId(id);
        viewBy.setCount(validCount.getCount());
        viewBy.setLabel(pathway.getName());
        viewBy.setExpand(!pathway.getChildren().isEmpty());
        return viewBy;
    }

    private UniPathway getRightLevelPathway(
            UniPathway pathway, Map<String, FacetField.Count> idCountMap) {
        List<UniPathway> children = pathway.getChildren();
        if (children.isEmpty()) return pathway;
        FacetField.Count count = idCountMap.get(pathway.getId());
        List<UniPathway> validChildren =
                children.stream()
                        .filter(val -> idCountMap.containsKey(val.getId()))
                        .collect(Collectors.toList());
        if (validChildren.isEmpty() || validChildren.size() > 1) {
            return pathway;
        } else {
            UniPathway child = validChildren.get(0);
            FacetField.Count childCount = idCountMap.get(child.getId());
            if (childCount.getCount() == count.getCount()) {
                return getRightLevelPathway(child, idCountMap);
            } else return pathway;
        }
    }
}
