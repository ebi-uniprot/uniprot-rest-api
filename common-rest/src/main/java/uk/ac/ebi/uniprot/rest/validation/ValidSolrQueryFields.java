package uk.ac.ebi.uniprot.uuw.uniprotkb.validation;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import uk.ac.ebi.uniprot.dataservice.client.SearchFieldType;
import uk.ac.ebi.uniprot.rest.validation.validator.SolrQueryFieldValidator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This is the solr query validator that is responsible to verify if the query has.
 *  - valid field names
 *  - valid query field type
 *  - if applicable, expected value format, for example, boolean, numbers, valid accession, etc...
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrQueryFields.QueryFieldValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrQueryFields {

    Class<? extends SolrQueryFieldValidator> fieldValidatorClazz();

    String message() default "{uk.ac.ebi.uniprot.uuw.advanced.search.uniprot.invalid.query.field}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class QueryFieldValidator implements ConstraintValidator<ValidSolrQueryFields, String> {

        SolrQueryFieldValidator fieldValidator = null;

        @Override
        public void initialize(ValidSolrQueryFields constraintAnnotation) {
            try {
                fieldValidator = constraintAnnotation.fieldValidatorClazz().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            try{
                StandardQueryParser qp = new StandardQueryParser();
                Query query = qp.parse(queryString,"");
                isValid = hasValidateQueryField(query,context);
                if(!isValid){
                    context.disableDefaultConstraintViolation();
                }
            }catch (Exception e){
                //Syntax error is validated by ValidSolrQuerySyntax
            }
            return isValid;
        }

        private boolean hasValidateQueryField(Query inputQuery,ConstraintValidatorContext context){
            boolean validField = true;
            if(inputQuery instanceof TermQuery){
                TermQuery termQuery = (TermQuery) inputQuery;
                String fieldName = termQuery.getTerm().field();
                String value = termQuery.getTerm().text();
                validField = isValidField(context, fieldName,SearchFieldType.TERM, value);
            }else if(inputQuery instanceof TermRangeQuery){
                TermRangeQuery rangeQuery = (TermRangeQuery) inputQuery;
                String fieldName = rangeQuery.getField();
                String value = rangeQuery.toString("");
                validField = isValidField(context, fieldName,SearchFieldType.RANGE, value);
            }else if(inputQuery instanceof PhraseQuery){
                PhraseQuery phraseQuery = (PhraseQuery) inputQuery;
                String fieldName = phraseQuery.getTerms()[0].field();
                String value = Arrays.stream(phraseQuery.getTerms()).map(Term::text).collect(Collectors.joining( " "));
                validField = isValidField(context, fieldName,SearchFieldType.TERM, value);
            }else if(inputQuery instanceof BooleanQuery){
                BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
                for (BooleanClause clause : booleanQuery.clauses()) {
                    if(!hasValidateQueryField(clause.getQuery(),context)){
                        validField = false;
                    }
                }
            }else{
                String errorMessage = "{uk.ac.ebi.uniprot.uuw.advanced.search.uniprot.invalid.query.type}";
                ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
                contextImpl.addMessageParameter("searchClass",inputQuery.getClass().getName());
                context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                validField = false;
            }
            return validField;
        }

        private boolean isValidField(ConstraintValidatorContext context, String fieldName, SearchFieldType type, String value) {
            boolean validField = true;
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            if(!fieldValidator.hasField(fieldName)){
                String errorMessage = fieldValidator.getInvalidFieldErrorMessage(fieldName);
                contextImpl.addMessageParameter("fieldName",fieldName);
                contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                validField = false;
            }else if(!fieldValidator.hasValidFieldType(fieldName,type)){
                String errorMessage = fieldValidator.getInvalidFieldTypeErrorMessage(fieldName,type);
                String expectedFieldType = fieldValidator.getExpectedSearchFieldType(fieldName).name().toLowerCase();
                contextImpl.addMessageParameter("fieldName",fieldName);
                contextImpl.addMessageParameter("fieldType",type.name().toLowerCase());
                contextImpl.addMessageParameter("expectedFieldType",expectedFieldType);
                contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                validField = false;
            }else if(!fieldValidator.hasValidFieldValue(fieldName,value)){
                String errorMessage = fieldValidator.getInvalidFieldValueErrorMessage(fieldName,value);
                contextImpl.addMessageParameter("fieldName",fieldName);
                contextImpl.addMessageParameter("fieldValue",value);
                contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                validField = false;
            }

            return validField;
        }

    }
}
