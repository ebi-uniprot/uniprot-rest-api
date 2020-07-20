package org.uniprot.api.rest.validation;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.core.util.Utils;

/**
 * It validates if the solr query has supported facet names in the query. @@author sahmad
 *
 * @created 29/06/2020
 */
@Constraint(validatedBy = ValidSolrQueryFacetFields.QueryFacetFieldValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrQueryFacetFields {

    Class<? extends FacetConfig> facetConfig();

    String message() default "{search.invalid.includeFacet}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class QueryFacetFieldValidator
            implements ConstraintValidator<ValidSolrQueryFacetFields, String> {

        private static final Logger LOGGER = getLogger(QueryFacetFieldValidator.class);
        @Autowired private HttpServletRequest request;
        @Autowired private ApplicationContext applicationContext;
        private Collection<String> facetNames;

        @Override
        public void initialize(ValidSolrQueryFacetFields constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            try {
                FacetConfig facetConfig =
                        applicationContext.getBean(constraintAnnotation.facetConfig());
                facetNames = facetConfig.getFacetNames();
            } catch (Exception e) {
                LOGGER.error("Unable to instantiate facet config", e);
                facetNames = new ArrayList<>();
            }
        }

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(queryString)) {
                try {
                    QueryParser qp = new QueryParser("", new WhitespaceAnalyzer());
                    qp.setAllowLeadingWildcard(true);
                    Query query = qp.parse(queryString);
                    if (!(query instanceof MatchAllDocsQuery)) {
                        isValid = hasValidFacetQueryField(query, context);
                    }
                    if (!isValid) {
                        context.disableDefaultConstraintViolation();
                    }
                } catch (Exception e) {
                    // Syntax error is validated by ValidSolrQuerySyntax
                }
            }
            return isValid;
        }

        private boolean hasValidFacetQueryField(
                Query inputQuery, ConstraintValidatorContext context) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            boolean validField = true;
            if (inputQuery instanceof TermQuery) {
                TermQuery termQuery = (TermQuery) inputQuery;
                String fieldName = termQuery.getTerm().field();
                validField = isValidFacet(fieldName, getFacetNames(), contextImpl);
            } else if (inputQuery instanceof WildcardQuery) {
                WildcardQuery wildcardQuery = (WildcardQuery) inputQuery;
                String fieldName = wildcardQuery.getTerm().field();
                validField = isValidFacet(fieldName, getFacetNames(), contextImpl);
            } else if (inputQuery instanceof PrefixQuery) {
                PrefixQuery prefixQuery = (PrefixQuery) inputQuery;
                String fieldName = prefixQuery.getPrefix().field();
                validField = isValidFacet(fieldName, getFacetNames(), contextImpl);
            } else if (inputQuery instanceof TermRangeQuery) {
                TermRangeQuery rangeQuery = (TermRangeQuery) inputQuery;
                String fieldName = rangeQuery.getField();
                validField = isValidFacet(fieldName, getFacetNames(), contextImpl);
            } else if (inputQuery instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) inputQuery;
                String fieldName = phraseQuery.getTerms()[0].field();
                validField = isValidFacet(fieldName, getFacetNames(), contextImpl);
            } else if (inputQuery instanceof BooleanQuery) {
                BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
                for (BooleanClause clause : booleanQuery.clauses()) {
                    if (!hasValidFacetQueryField(clause.getQuery(), context)) {
                        validField = false;
                    }
                }
            } else {
                validField = false;
            }
            return validField;
        }

        private boolean isValidFacet(
                String facetName,
                Collection<String> allowedFacetNames,
                ConstraintValidatorContextImpl contextImpl) {
            boolean isValid = allowedFacetNames.contains(facetName);
            if (!isValid) {
                buildInvalidFacetNameMessage(facetName, allowedFacetNames, contextImpl);
            }
            return isValid;
        }

        void buildInvalidFacetNameMessage(
                String facetName,
                Collection<String> allowedFacetNames,
                ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.facet.name}";
            contextImpl.addMessageParameter("0", facetName);
            contextImpl.addMessageParameter("1", "[" + String.join(", ", allowedFacetNames) + "]");
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        HttpServletRequest getRequest() {
            return request;
        }

        Collection<String> getFacetNames() {
            return facetNames;
        }
    }
}
