package uk.ac.ebi.uniprot.uuw.advanced.search.validation;


import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is the solr query solr validator is responsible to verify if the sort field parameter has valid field names
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidSolrSortFields.SortFieldValidatorImpl.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSolrSortFields {

    Class<? extends Enum<?>> sortFieldEnumClazz();

    String message() default "{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.sort}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class SortFieldValidatorImpl implements ConstraintValidator <ValidSolrSortFields,String> {

        private static final String SORT_FORMAT = "^([\\w]+)\\s([\\w]+)(\\s*,\\s*([\\w]+)\\s([\\w]+))*$";
        private static final String SORT_ORDER = "^asc|desc$";
        private List<String> valueList = null;

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            boolean result = true;
            if(value != null) {
                value = value.toLowerCase();
                if(value.matches(SORT_FORMAT)){
                    Pattern pattern = Pattern.compile(SORT_FORMAT);
                    Matcher matcher = pattern.matcher(value);
                    if(matcher.matches()){
                        int index = 0;
                        List<String> groups = getMatchGroupList(matcher);
                        while (groups.size() > index){
                            String sortField = groups.get(++index);
                            if(!valueList.contains(sortField)){
                                String errorMessage = "{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.sort.field}";
                                contextImpl.addMessageParameter("sortField",sortField);
                                contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                                result = false;
                            }

                            String sortOrder = groups.get(++index);
                            if(!sortOrder.matches(SORT_ORDER)){
                                String errorMessage = "{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.sort.order}";
                                contextImpl.addMessageParameter("sortOrder",sortOrder);
                                contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
                                result = false;
                            }
                            index++; //the comma is another group
                        }
                        if(!result){
                            contextImpl.disableDefaultConstraintViolation();
                        }
                    }
                }
            }
            return result;
        }

        private List<String> getMatchGroupList(Matcher matcher) {
            List<String> groups = new ArrayList<>();
            for (int i=0;i <= matcher.groupCount() ;i++){
                if(matcher.group(i) != null) {
                    groups.add(matcher.group(i));
                }
            }
            return groups;
        }

        @Override
        public void initialize(ValidSolrSortFields constraintAnnotation) {
            valueList = new ArrayList<>();
            Class<? extends Enum<?>> enumClass = constraintAnnotation.sortFieldEnumClazz();

            Enum[] enumValArr = enumClass.getEnumConstants();

            for(Enum enumVal : enumValArr) {
                valueList.add(enumVal.name().toLowerCase());
            }

        }
    }
}
