package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

@Constraint(validatedBy = ValidUniqueIdList.AccessionListValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUniqueIdList {
    String message() default "invalid id list";

    UniProtDataType uniProtDataType();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AccessionListValidator implements ConstraintValidator<ValidUniqueIdList, String> {

        @Value("${ids.max.length}")
        private String maxLength;

        private UniProtDataType dataType;

        @Override
        public void initialize(ValidUniqueIdList constraintAnnotation) {
            this.dataType = constraintAnnotation.uniProtDataType();
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notNullNotEmpty(value)) {
                ConstraintValidatorContextImpl contextImpl =
                        (ConstraintValidatorContextImpl) context;
                // verify if id is valid.
                String[] ids = value.split("\\s*,\\s*");
                for (String id : ids) {
                    if (!isIdValid(id)) {
                        buildInvalidAccessionMessage(id, contextImpl);
                        isValid = false;
                    }
                }
                if (ids.length > getMaxLength()) {
                    buildInvalidAccessionLengthMessage(contextImpl);
                    isValid = false;
                }
                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        private boolean isIdValid(String id){
            switch (getDataType()){
                case UNIPROTKB:
                    return id.toUpperCase().matches(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);
                case UNIPARC:
                    return id.toUpperCase().matches(FieldRegexConstants.UNIPARC_UPI_REGEX);
                case UNIREF:
                    return id.matches(FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX);
                default:
                    throw new IllegalArgumentException("Unknown UniProtDataType " + this.dataType);
            }
        }

        void buildInvalidAccessionMessage(
                String accession, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{ids.invalid.ids.value}";
            contextImpl.addMessageParameter("0", accession);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        void buildInvalidAccessionLengthMessage(ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{ids.invalid.ids.size}";
            contextImpl.addMessageParameter("0", getMaxLength());
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        int getMaxLength() {
            return Integer.parseInt(maxLength);
        }

        UniProtDataType getDataType(){
            return this.dataType;
        }
    }
}
