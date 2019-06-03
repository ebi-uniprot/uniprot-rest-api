package uk.ac.ebi.uniprot.api.rest.validation;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import uk.ac.ebi.uniprot.common.Utils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
        public void initialize(ValidSolrQuerySyntax constraintAnnotation) {
        }

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notEmpty(queryString)) {
                try {
                    StandardQueryParser standardQueryParser = new StandardQueryParser();
                    standardQueryParser.setAllowLeadingWildcard(true);
                    standardQueryParser.parse(queryString, "");
                } catch (QueryNodeException e) {
                    isValid = false;
                }
            }
            return isValid;
        }
    }

}
