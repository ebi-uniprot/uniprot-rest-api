package org.uniprot.api.rest.validation;

import static org.uniprot.store.search.SolrQueryUtil.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.util.StringUtils;
import org.uniprot.core.util.Utils;

/**
 * This is the solr query validator that is responsible to verify its syntax.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrQuerySyntax.QuerySyntaxValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrQuerySyntax {

    String message() default "{search.invalid.query}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class QuerySyntaxValidator implements ConstraintValidator<ValidSolrQuerySyntax, String> {

        @Override
        public void initialize(ValidSolrQuerySyntax constraintAnnotation) {}

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(queryString)) {
                try {
                    QueryParser qp = new QueryParser("", new WhitespaceAnalyzer());
                    qp.setAllowLeadingWildcard(true);
                    queryString = escapeSpecialCharacters(queryString);
                    Query parsedQuery = qp.parse(queryString);
                    if (!isValidWildcardQuery(parsedQuery)) {
                        String errorMessage = "{search.invalid.query.wildcard}";
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(errorMessage)
                                .addConstraintViolation();
                        isValid = false;
                    }
                } catch (ParseException e) {
                    isValid = false;
                }
            }
            return isValid;
        }

        private boolean isValidWildcardQuery(Query inputQuery) {
            boolean isValid = true;
            if (inputQuery instanceof WildcardQuery) {
                WildcardQuery wildcardQuery = (WildcardQuery) inputQuery;
                String value = wildcardQuery.getTerm().text();
                if (hasMultipleMiddleAsteriskWildcard(value)) {
                    isValid = false;
                }
            } else if (inputQuery instanceof BooleanQuery) {
                BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
                for (BooleanClause clause : booleanQuery.clauses()) {
                    if (!isValidWildcardQuery(clause.getQuery())) {
                        isValid = false;
                    }
                }
            }
            return isValid;
        }

        private boolean hasMultipleMiddleAsteriskWildcard(String value) {
            String strippedValue = stripWildcard(value);
            return StringUtils.countOccurrencesOf(strippedValue, "*") >= 2;
        }

        private static String stripWildcard(String value) {
            while (value.startsWith("*")) {
                value = value.substring(1);
            }
            while (value.endsWith("*")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
    }
}
