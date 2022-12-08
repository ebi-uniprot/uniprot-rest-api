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

    String message() default "{search.invalid.syntax.query}";

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
                    queryString = replaceForwardSlashes(queryString);
                    qp.parse(queryString);
                } catch (ParseException e) {
                    isValid = false;
                }
            }
            return isValid;
        }
    }
}
