package org.uniprot.api.uniprotkb.view;

import java.util.List;

import lombok.Data;

@Data
public class Taxonomies {
    private List<TaxonomyNode> taxonomies;

    private PageInformation pageInfo;
}
