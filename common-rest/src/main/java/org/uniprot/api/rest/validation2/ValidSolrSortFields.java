package org.uniprot.api.rest.validation2;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.store.search.domain2.SearchField;
import org.uniprot.store.search.domain2.SearchFields;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is the solr query solr validator is responsible to verify if the sort field parameter has
 * valid field names
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrSortFields.SortFieldValidatorImpl.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrSortFields {

    Class<? extends Enum<?>> sortFieldEnumClazz();

    String message() default "{search.invalid.sort}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SortFieldValidatorImpl implements ConstraintValidator<ValidSolrSortFields, String> {

        private static final String SORT_FORMAT =
                "^([\\w]+)\\s([\\w]+)(\\s*,\\s*([\\w]+)\\s([\\w]+))*$";
        private static final String SORT_ORDER = "^asc|desc$";
        private List<String> valueList;

        @Override
        public void initialize(ValidSolrSortFields constraintAnnotation) {
            valueList = new ArrayList<>();
            Class<? extends Enum<?>> enumClass = constraintAnnotation.sortFieldEnumClazz();

            SearchFields searchFields = (SearchFields) enumClass.getEnumConstants()[0];
            valueList =
                    searchFields.getSearchFields().stream()
                            .filter(field -> field.getSortField().isPresent())
                            .map(SearchField::getName)
                            .collect(Collectors.toList());
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            boolean result = true;
            if (value != null) {
                value = value.toLowerCase();
                if (value.matches(SORT_FORMAT)) {
                    Pattern pattern = Pattern.compile(SORT_FORMAT);
                    Matcher matcher = pattern.matcher(value);
                    // TODO: 28/11/2019 use regex to validate format, then split string on , to get parts.
                    if (matcher.matches()) {
                        int index = 0;
                        List<String> groups = getMatchGroupList(matcher);
                        while (groups.size() > index) {
                            String sortField = groups.get(++index);
                            if (!valueList.contains(sortField)) {
                                addInvalidSortFieldErrorMessage(contextImpl, sortField);
                                result = false;
                            }

                            String sortOrder = groups.get(++index);
                            if (!sortOrder.matches(SORT_ORDER)) {
                                addInvalidSortOrderErrorMessage(contextImpl, sortOrder);
                                result = false;
                            }
                            index++; // the comma is another group
                        }
                        if (!result && contextImpl != null) {
                            contextImpl.disableDefaultConstraintViolation();
                        }
                    }
                } else {
                    addInvalidSortFormatErrorMessage(contextImpl, value);
                    result = false;
                }
            }
            return result;
        }

        public void addInvalidSortFormatErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String value) {
            String errorMessage = "{search.invalid.sort.format}";
            contextImpl.addMessageParameter("0", value);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addInvalidSortOrderErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String sortOrder) {
            String errorMessage = "{search.invalid.sort.order}";
            contextImpl.addMessageParameter("0", sortOrder);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        public void addInvalidSortFieldErrorMessage(
                ConstraintValidatorContextImpl contextImpl, String sortField) {
            String errorMessage = "{search.invalid.sort.field}";
            contextImpl.addMessageParameter("0", sortField);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        private List<String> getMatchGroupList(Matcher matcher) {
            List<String> groups = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    groups.add(matcher.group(i));
                }
            }
            return groups;
        }
    }
}
