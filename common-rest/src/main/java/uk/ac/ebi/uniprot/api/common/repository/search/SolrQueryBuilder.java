package uk.ac.ebi.uniprot.api.common.repository.search;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;
import uk.ac.ebi.uniprot.common.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for parsing a request query string and creating a corresponding {@link SimpleQuery}
 *
 * @author lgonzales
 */
public class SolrQueryBuilder {

    private String query;
    private GenericFacetConfig facetConfig;
    private List<String> facets;
    private List<SimpleQuery> filterQuery = new ArrayList<>();
    private Sort sort;
    private Query.Operator defaultOperator = Query.Operator.AND;

    public SolrQueryBuilder(){
    }

    public SolrQueryBuilder query(String query){
        this.query = query;
        return this;
    }

    public SolrQueryBuilder facets(List<String> facets){
        this.facets = facets;
        return this;
    }

    public SolrQueryBuilder facetConfig(GenericFacetConfig facetConfig){
        this.facetConfig = facetConfig;
        return this;
    }

    public SolrQueryBuilder filterQuery(List<SimpleQuery> filterQuery){
        this.filterQuery = Utils.nonNullList(filterQuery);
        return this;
    }

    public SolrQueryBuilder addFilterQuery(SimpleQuery filterQuery){
        Utils.nonNullAdd(filterQuery,this.filterQuery);
        return this;
    }

    public SolrQueryBuilder defaultOperator(Query.Operator defaultOperator){
        this.defaultOperator  =defaultOperator;
        return this;
    }

    public SolrQueryBuilder addSort(Sort sort) {
        if(this.sort == null){
            this.sort = sort;
        }else{
            this.sort.and(sort);
        }
        return this;
    }

    public SimpleQuery build() {
        SimpleQuery simpleQuery = new SimpleQuery(query);
        if (Utils.notEmpty(facets)) {
            simpleQuery = getSimpleFacetQuery(simpleQuery);
        }
        filterQuery.forEach(simpleQuery::addFilterQuery);

        simpleQuery.addSort(this.sort);
        simpleQuery.setDefaultOperator(defaultOperator);
        return simpleQuery;
    }

    private SimpleFacetQuery getSimpleFacetQuery(SimpleQuery simpleQuery) {
        SimpleFacetQuery simpleFacetQuery = new SimpleFacetQuery(simpleQuery.getCriteria());

        FacetOptions facetOptions = new FacetOptions();
        facetOptions.addFacetOnFlieldnames(facets);
        facetOptions.setFacetMinCount(facetConfig.getMincount());
        facetOptions.setFacetLimit(facetConfig.getLimit());
        simpleFacetQuery.setFacetOptions(facetOptions);

        return simpleFacetQuery;
    }
}
