package uk.ac.ebi.uniprot.uuw.advanced.search.query;

import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;

/**
 * This class is responsible to parse request query string to {@link SimpleQuery}
 *
 * @author lgonzales
 */
public class SolrQueryBuilder {

    private final String query;
    private final UniprotFacetConfig uniprotFacetConfig;

    private SolrQueryBuilder(String query, UniprotFacetConfig uniprotFacetConfig){
        this.query = query;
        this.uniprotFacetConfig = uniprotFacetConfig;
    }

    public static SolrQueryBuilder of(String query, UniprotFacetConfig uniprotFacetConfig){
        return new SolrQueryBuilder(query,uniprotFacetConfig);
    }

    public SimpleQuery build(){
        SimpleQuery simpleQuery = new SimpleQuery(query);
        simpleQuery = getSimpleFacetQuery(simpleQuery);

        return simpleQuery;
    }

    private SimpleFacetQuery getSimpleFacetQuery(SimpleQuery simpleQuery){
        SimpleFacetQuery simpleFacetQuery = new SimpleFacetQuery(simpleQuery.getCriteria());

        FacetOptions facetOptions = new FacetOptions();
        facetOptions.addFacetOnFlieldnames(uniprotFacetConfig.getUniprot().keySet());
        facetOptions.setFacetMinCount(uniprotFacetConfig.getMincount());
        facetOptions.setFacetLimit(uniprotFacetConfig.getLimit());
        simpleFacetQuery.setFacetOptions(facetOptions);

        return simpleFacetQuery;
    }

}
