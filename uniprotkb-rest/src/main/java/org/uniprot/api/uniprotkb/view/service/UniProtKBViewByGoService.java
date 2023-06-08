package org.uniprot.api.uniprotkb.view.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.api.uniprotkb.view.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UniProtKBViewByGoService extends UniProtKBViewByService<GoRelation> {
    private static final String GO_PREFIX = "GO:";
    private final GoService goService;

    public UniProtKBViewByGoService(GoService goService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.goService = goService;
    }

    @Override
    protected List<GoRelation> getChildren(String parent) {
        String parentGo = addGoPrefix(parent);
        return goService
                .getChildren(parentGo)
                .map(GoTerm::getChildren)
                .orElse(Collections.emptyList());
    }

    @Override
    protected Map<String, String> getFacetFields(List<GoRelation> entries) {
        String goIds =
                entries.stream()
                        .map(GoRelation::getId)
                        .map(this::removeGoPrefix)
                        .collect(Collectors.joining(","));
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}go_id", goIds));
    }

    @Override
    protected String getId(GoRelation entry) {
        return entry.getId();
    }

    @Override
    protected String getLabel(GoRelation entry) {
        return entry.getName();
    }

    @Override
    protected String getFacetName(FacetField.Count fc) {
        return addGoPrefix(fc.getName());
    }

    /*@Override
    protected ViewByResult getViewBys(
            List<FacetField.Count> facetCounts, List<GoRelation> entries, List<GoRelation> ancestors, String query) {
        Map<String, GoRelation> goRelationMap =
                entries.stream().collect(Collectors.toMap(GoRelation::getId, Function.identity()));
        return new ViewByResult(getAncestors(ancestors), facetCounts.stream()
                .map(fc -> getViewBy(fc, goRelationMap.get(addGoPrefix(fc.getName())), query))
                .sorted(ViewBy.SORT_BY_LABEL_IGNORE_CASE)
                .collect(Collectors.toList()));
    }*/

    private List<Ancestor> getAncestors(List<GoRelation> gos) {
        return gos.stream().map(go -> AncestorImpl.builder().id(go.getId()).label(go.getName()).build())
                .collect(Collectors.toList());
    }

    private ViewBy getViewBy(FacetField.Count count, GoRelation goRelation, String query) {
        return ViewByImpl.builder()
                .id(goRelation.getId())
                .label(goRelation.getName())
                .count(count.getCount())
                .expand(hasChildren(count, query))
                .build();
    }

    private String removeGoPrefix(String go) {
        return !StringUtils.isEmpty(go) && go.startsWith(GO_PREFIX)
                ? go.substring(GO_PREFIX.length())
                : go;
    }

    private String addGoPrefix(String go) {
        return !StringUtils.isEmpty(go) && !go.startsWith(GO_PREFIX) ? GO_PREFIX + go : go;
    }
}
