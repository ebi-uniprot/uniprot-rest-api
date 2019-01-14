package uk.ac.ebi.uniprot.common.repository.search;

import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.common.repository.search.facet.GenericFacetConfig;

/**
 * This class is responsible for parsing a request query string and creating a corresponding {@link SimpleQuery}
 *
 * @author lgonzales
 */
public class SolrQueryBuilder {

    private final String query;
    private final GenericFacetConfig facetConfig;

    private SolrQueryBuilder(String query, GenericFacetConfig facetConfig) {
        this.query = query;
        this.facetConfig = facetConfig;
    }

    public static SolrQueryBuilder of(String query) {
        return new SolrQueryBuilder(query, null);
    }

    public static SolrQueryBuilder of(String query, GenericFacetConfig uniprotFacetConfig) {
        return new SolrQueryBuilder(query, uniprotFacetConfig);
    }

    public SimpleQuery build() {
        SimpleQuery simpleQuery = new SimpleQuery(query);
        if (facetConfig != null) {
            simpleQuery = getSimpleFacetQuery(simpleQuery);
        }

        return simpleQuery;
    }

    private SimpleFacetQuery getSimpleFacetQuery(SimpleQuery simpleQuery) {
        SimpleFacetQuery simpleFacetQuery = new SimpleFacetQuery(simpleQuery.getCriteria());

        FacetOptions facetOptions = new FacetOptions();
        facetOptions.addFacetOnFlieldnames(facetConfig.getFacetNames());
        facetOptions.setFacetMinCount(facetConfig.getMincount());
        facetOptions.setFacetLimit(facetConfig.getLimit());
        simpleFacetQuery.setFacetOptions(facetOptions);

        return simpleFacetQuery;
    }
}
