package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import lombok.Builder;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 10/10/18
 *
 * @author Edd
 */
@Builder
public class SortCriteria {
    private List<SortCriterion> criteriaList;

    public static class SortCriteriaBuilder {
        private List<SortCriterion> criteriaList = new ArrayList<>();
        public SortCriteriaBuilder addCriterion(String field, SolrQuery.ORDER order) {
            this.criteriaList.add(SortCriterion.builder().field(field).order(order).build());
            return this;
        }
    }

    public String toString() {
        return criteriaList.stream().map(SortCriterion::toString).collect(Collectors.joining(", "));
    }

    @Builder
    private static class SortCriterion {
        private String field;
        private SolrQuery.ORDER order;

        public String toString() {
            return field + " " + order.name();
        }
    }
}
