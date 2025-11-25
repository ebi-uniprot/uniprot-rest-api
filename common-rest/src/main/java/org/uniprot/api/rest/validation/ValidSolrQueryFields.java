package org.uniprot.api.rest.validation;

import static org.slf4j.LoggerFactory.getLogger;
import static org.uniprot.store.config.UniProtDataType.UNIPROTKB;
import static org.uniprot.store.search.SolrQueryUtil.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.config.searchfield.model.SearchFieldType;

/**
 * This is the solr query validator that is responsible to verify if the query has. - valid field
 * names - valid query field type - if applicable, expected value format, for example, boolean,
 * numbers, valid accession, etc...
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrQueryFields.QueryFieldValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrQueryFields {

    UniProtDataType uniProtDataType();

    String messagePrefix();

    String message() default "{search.uniprot.invalid.query.field}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class QueryFieldValidator implements ConstraintValidator<ValidSolrQueryFields, String> {

        private static final Logger LOGGER = getLogger(QueryFieldValidator.class);
        private static final String DEFAULT_FIELD_NAME = "default_field";
        private String messagePrefix;
        private SearchFieldConfig searchFieldConfig;

        @Autowired private ApplicationContext applicationContext;

        private Map<String, String> whiteListFields;
        private UniProtDataType uniProtDataType;

        @Override
        public void initialize(ValidSolrQueryFields constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            try {
                this.uniProtDataType = constraintAnnotation.uniProtDataType();
                this.searchFieldConfig =
                        SearchFieldConfigFactory.getSearchFieldConfig(uniProtDataType);
                this.messagePrefix = constraintAnnotation.messagePrefix();

                WhitelistFieldConfig whiteListFieldConfig = getWhitelistFieldConfig();
                whiteListFields =
                        whiteListFieldConfig
                                .getField()
                                .getOrDefault(
                                        uniProtDataType.name().toLowerCase(), new HashMap<>());
            } catch (Exception e) {
                LOGGER.error("Error initializing QueryFieldValidator", e);
                whiteListFields = new HashMap<>();
            }
        }

        WhitelistFieldConfig getWhitelistFieldConfig() {
            return applicationContext.getBean(WhitelistFieldConfig.class);
        }

        @Override
        public boolean isValid(String queryString, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(queryString)) {
                try {
                    Query query = getParsedQuery(queryString);

                    if (!(query instanceof MatchAllDocsQuery)) {
                        isValid = hasValidQueryField(query, context);
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

        public void addFieldValueErrorMessage(
                String fieldName, String value, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage =
                    "{" + messagePrefix + ".invalid.query.field.value." + fieldName + "}";
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.addMessageParameter("1", value);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addFieldTypeErrorMessage(
                String fieldName,
                SearchFieldType type,
                ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.field.type}";
            String expectedFieldType =
                    getFieldByName(fieldName).getFieldType().name().toLowerCase();
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.addMessageParameter("1", type.name().toLowerCase());
            contextImpl.addMessageParameter("2", expectedFieldType);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addFieldNameErrorMessage(
                String fieldName, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.field}";
            contextImpl.addMessageParameter("0", fieldName);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addQueryTypeErrorMessage(Query inputQuery, ConstraintValidatorContext context) {
            String errorMessage = "{" + messagePrefix + ".invalid.query.type}";
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            contextImpl.addMessageParameter("0", inputQuery.getClass().getName());
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        private boolean hasValidQueryField(Query inputQuery, ConstraintValidatorContext context) {
            boolean validField = true;
            if (inputQuery instanceof TermQuery) {
                TermQuery termQuery = (TermQuery) inputQuery;
                String fieldName = termQuery.getTerm().field();
                String value = termQuery.getTerm().text();
                validField = isValidField(context, fieldName, SearchFieldType.GENERAL, value);
            } else if (inputQuery instanceof WildcardQuery) {
                WildcardQuery wildcardQuery = (WildcardQuery) inputQuery;
                String fieldName = wildcardQuery.getTerm().field();
                String value = wildcardQuery.getTerm().text();
                validField = isValidField(context, fieldName, SearchFieldType.GENERAL, value);
            } else if (inputQuery instanceof PrefixQuery) {
                PrefixQuery prefixQuery = (PrefixQuery) inputQuery;
                String fieldName = prefixQuery.getPrefix().field();
                String value = prefixQuery.getPrefix().text();
                validField = isValidField(context, fieldName, SearchFieldType.GENERAL, value);
            } else if (inputQuery instanceof TermRangeQuery) {
                TermRangeQuery rangeQuery = (TermRangeQuery) inputQuery;
                validField = isValidField(context, rangeQuery);
            } else if (inputQuery instanceof PhraseQuery) {
                PhraseQuery phraseQuery = (PhraseQuery) inputQuery;
                String fieldName = phraseQuery.getTerms()[0].field();
                String value =
                        Arrays.stream(phraseQuery.getTerms())
                                .map(Term::text)
                                .collect(Collectors.joining(" "));
                validField = isValidField(context, fieldName, SearchFieldType.GENERAL, value);
            } else if (inputQuery instanceof BooleanQuery) {
                BooleanQuery booleanQuery = (BooleanQuery) inputQuery;
                for (BooleanClause clause : booleanQuery.clauses()) {
                    if (!hasValidQueryField(clause.getQuery(), context)) {
                        validField = false;
                    }
                }
            } else {
                addQueryTypeErrorMessage(inputQuery, context);
                validField = false;
            }
            return validField;
        }

        private boolean isValidField(
                ConstraintValidatorContext context, TermRangeQuery rangeQuery) {
            String fieldName = rangeQuery.getField();
            String fieldValue = rangeQuery.toString(fieldName);
            if (UNIPROTKB.equals(uniProtDataType) && "length".equals(fieldName)) {
                int lower =
                        Integer.parseInt(
                                new String(
                                        rangeQuery.getLowerTerm().bytes, StandardCharsets.UTF_8));
                if (lower <= 0) {
                    addFieldValueErrorMessage(
                            fieldName, fieldValue, (ConstraintValidatorContextImpl) context);
                    return false;
                }
            }
            return isValidField(context, fieldName, SearchFieldType.RANGE, fieldValue);
        }

        private boolean isValidField(
                ConstraintValidatorContext context,
                String fieldName,
                SearchFieldType type,
                String value) {
            boolean validField = true;
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            boolean fieldExists = this.searchFieldConfig.searchFieldItemExists(fieldName);
            if (!fieldExists
                    && !fieldName.equals(DEFAULT_FIELD_NAME)
                    && !isWhiteListField(fieldName, value)) {
                addFieldNameErrorMessage(fieldName, contextImpl);
                validField = false;
            } else if (fieldExists) {
                SearchFieldType fieldType =
                        this.searchFieldConfig.getFieldTypeBySearchFieldName(fieldName);
                if (!Objects.equals(type, fieldType)) {
                    addFieldTypeErrorMessage(fieldName, type, contextImpl);
                    validField = false;
                } else if (!searchFieldConfig.isSearchFieldValueValid(fieldName, value)) {
                    addFieldValueErrorMessage(fieldName, value, contextImpl);
                    validField = false;
                }
            }
            return validField;
        }

        private boolean isWhiteListField(String fieldName, String value) {
            return whiteListFields.containsKey(fieldName.toLowerCase())
                    && value.matches(whiteListFields.get(fieldName.toLowerCase()));
        }

        private SearchFieldItem getFieldByName(String fieldName) {
            return this.searchFieldConfig.getSearchFieldItemByName(fieldName);
        }

        public static Query getParsedQuery(String queryString) {
            try {
                QueryParser qp = new QueryParser(DEFAULT_FIELD_NAME, new WhitespaceAnalyzer());
                qp.setAllowLeadingWildcard(true);
                queryString = escapeSpecialCharacters(queryString);
                return qp.parse(queryString);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
