package org.uniprot.api.rest.validation;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

@Constraint(validatedBy = ValidGetByIdsRequest.GetByIdsRequestValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGetByIdsRequest {

    String message() default "invalid get by ids request";

    String accessions() default "accessions";

    String facets() default "facets";

    String sort() default "sort";

    String query() default "query";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    class GetByIdsRequestValidator implements ConstraintValidator<ValidGetByIdsRequest, Object> {
        private String accessions;
        private String facets;
        private String query;

        private String sort;

        @Autowired private HttpServletRequest request;

        @Override
        public void initialize(ValidGetByIdsRequest constraintAnnotation) {
            this.accessions = constraintAnnotation.accessions();
            this.facets = constraintAnnotation.facets();
            this.sort = constraintAnnotation.sort();
            this.query = constraintAnnotation.query();
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isValid = true;
            try {
                String passedAccessions = BeanUtils.getProperty(value, this.accessions);
                String passedFacets = BeanUtils.getProperty(value, this.facets);
                String passedSort = BeanUtils.getProperty(value, this.sort);
                String passedQuery = BeanUtils.getProperty(value, this.query);

                if (Utils.notNullNotEmpty(passedAccessions)) {
                    isValid =
                            validateAccessionsWithSequenceRange(
                                    passedAccessions,
                                    passedFacets,
                                    passedQuery,
                                    passedSort,
                                    context);
                }

                if (!isValid && context != null) {
                    context.disableDefaultConstraintViolation();
                }
            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }

            return isValid;
        }

        boolean validateAccessionsWithSequenceRange(
                String accessions,
                String facets,
                String query,
                String sort,
                ConstraintValidatorContext context) {
            boolean isValid = true;
            ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
            if (sequenceRangeExists(accessions)) {
                if (Objects.nonNull(facets)) {
                    isValid = false;
                    String errMsg = "facets not supported with sequence range";
                    contextImpl
                            .buildConstraintViolationWithTemplate(errMsg)
                            .addConstraintViolation();
                }
                if (Objects.nonNull(query)) {
                    isValid = false;
                    String errMsg = "query not supported with sequence range";
                    contextImpl
                            .buildConstraintViolationWithTemplate(errMsg)
                            .addConstraintViolation();
                }
                if (Objects.nonNull(sort)) {
                    isValid = false;
                    String errMsg = "sort not supported with sequence range";
                    contextImpl
                            .buildConstraintViolationWithTemplate(errMsg)
                            .addConstraintViolation();
                }
            }
            return isValid;
        }

        private boolean sequenceRangeExists(String ids) {
            for (String passedId : ids.split(",")) {
                String sanitisedId = passedId.strip().toUpperCase();
                if (UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX.matcher(sanitisedId).matches()) {
                    return true;
                }
            }
            return false;
        }
    }
}
