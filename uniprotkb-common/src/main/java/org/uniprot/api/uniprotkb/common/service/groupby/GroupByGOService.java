package org.uniprot.api.uniprotkb.common.service.groupby;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniprotkb.common.service.go.GOService;
import org.uniprot.api.uniprotkb.common.service.go.model.GoRelation;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupByResult;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

@Service
public class GroupByGOService extends GroupByService<GoRelation> {
    protected static final String GO_PREFIX = "GO:";
    private final GOService goService;

    public GroupByGOService(GOService goService, UniProtEntryService uniProtEntryService) {
        super(uniProtEntryService);
        this.goService = goService;
    }

    @Override
    public List<GoRelation> getChildEntries(String parent) {
        String parentGo = addGoPrefix(parent);
        return goService.getChildren(parentGo);
    }

    @Override
    public Map<String, String> getFacetParams(List<GoRelation> entries) {
        String goIds =
                entries.stream()
                        .map(GoRelation::getId)
                        .map(this::removeGoPrefix)
                        .collect(Collectors.joining(","));
        return Map.of(FacetParams.FACET_FIELD, String.format("{!terms='%s'}go_id", goIds));
    }

    @Override
    protected boolean areEqualIds(String parent, String facetId) {
        return Objects.equals(addGoPrefix(parent), facetId);
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
    public GroupByResult getGroupByResult(
            List<FacetField.Count> facetCounts,
            List<GoRelation> goRelations,
            List<GoRelation> ancestorEntries,
            String parentId,
            List<FacetField.Count> parentFacetCounts,
            String query) {
        Map<String, GoRelation> idEntryMap =
                goRelations.stream().collect(Collectors.toMap(this::getId, Function.identity()));
        return getGroupByResult(
                facetCounts, idEntryMap, ancestorEntries, parentId, parentFacetCounts, query);
    }

    @Override
    protected GoRelation getEntryById(String id) {
        return goService.getGoRelation(id).orElseThrow();
    }

    @Override
    protected String getFacetId(FacetField.Count fc) {
        return addGoPrefix(fc.getName());
    }

    private String removeGoPrefix(String goIdWithPrefix) {
        return !StringUtils.isEmpty(goIdWithPrefix) && goIdWithPrefix.startsWith(GO_PREFIX)
                ? goIdWithPrefix.substring(GO_PREFIX.length())
                : goIdWithPrefix;
    }

    private String addGoPrefix(String goIdWithoutPrefix) {
        return !StringUtils.isEmpty(goIdWithoutPrefix) && !goIdWithoutPrefix.startsWith(GO_PREFIX)
                ? GO_PREFIX + goIdWithoutPrefix
                : goIdWithoutPrefix;
    }
}
