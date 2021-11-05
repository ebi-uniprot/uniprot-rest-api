package org.uniprot.api.rest.validation;

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
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
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

        /**
         * Although '/' is a special lucene character, the old uniprot website allows users to
         * *not* escape it. That is, they allow queries like, "hello/world", to pass unescaped through to lucene.
         * Therefore, in order to allow it, we should escape it here, allowing validation to "ignore"
         * the forward slash.
         *
         * @param queryString the query string from the client
         * @return the query string with forward slashes appropriately escaped
         */
        public static String replaceForwardSlashes(String queryString) {
            StringBuilder sb = new StringBuilder();
            char prev = '\u00A0'; // an unprintable character very unlikely to be input
            for (int i = 0; i < queryString.length(); i++) {
                char curr = queryString.charAt(i);
                if (curr == '/' && prev != '\\') {
                    sb.append("\\/");
                } else {
                    sb.append(curr);
                }
                prev = curr;
            }

            return sb.toString();
        }
    }
}
