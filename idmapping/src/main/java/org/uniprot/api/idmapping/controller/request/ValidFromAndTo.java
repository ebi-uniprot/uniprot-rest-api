package org.uniprot.api.idmapping.controller.request;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.ACC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.GENENAME_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF100_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF50_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF90_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UPARC_STR;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.BeanUtils;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * @author sahmad
 * @created 01/03/2021
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidFromAndTo.ValidFromAndToValidator.class)
public @interface ValidFromAndTo {
    String message() default "{idmapping.invalid.from.to}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String from() default "from";

    String to() default "to";

    String taxId() default "taxId";

    @Slf4j
    class ValidFromAndToValidator implements ConstraintValidator<ValidFromAndTo, Object> {
        private String fromFieldName;
        private String toFieldName;
        private String taxIdFieldName;

        private static final Set<String> FROM_WITH_DIFFERENT_TO =
                Set.of(
                        ACC_ID_STR,
                        UPARC_STR,
                        UNIREF50_STR,
                        UNIREF90_STR,
                        UNIREF100_STR,
                        GENENAME_STR);
        private static final Set<String> FROM_RULE_101 =
                Set.of(UPARC_STR, UNIREF50_STR, UNIREF90_STR, UNIREF100_STR);

        @Override
        public void initialize(ValidFromAndTo constraintAnnotation) {
            this.fromFieldName = constraintAnnotation.from();
            this.toFieldName = constraintAnnotation.to();
            this.taxIdFieldName = constraintAnnotation.taxId();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isValid;

            try {
                String fromValue = BeanUtils.getProperty(value, this.fromFieldName);
                String toValue = BeanUtils.getProperty(value, this.toFieldName);
                String taxIdValue = BeanUtils.getProperty(value, this.taxIdFieldName);
                isValid = isValidPair(fromValue, toValue, taxIdValue, context);
            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }
            return isValid;
        }

        private boolean isValidPair(
                String from, String to, String taxId, ConstraintValidatorContext context) {
            boolean isValid = true;
            boolean isTaxIdPassed = Utils.notNullNotEmpty(taxId);
            if (!FROM_WITH_DIFFERENT_TO.contains(from)) {
                isValid = isUniProtKBOrSwissProt(to);
            }
            if (FROM_RULE_101.contains(from)) {
                isValid = isUniProtKBOrSwissProt(to) || from.equals(to);
            }

            if (!GENENAME_STR.equals(from)
                    && isTaxIdPassed) { // if taxId is passed for other than gene name, add error
                buildErrorMessage(isValid, context);
                isValid = false;
            }

            return isValid;
        }

        private boolean isUniProtKBOrSwissProt(String to) {
            return IdMappingFieldConfig.ACC_STR.equals(to)
                    || IdMappingFieldConfig.SWISSPROT_STR.equals(to);
        }

        protected void buildErrorMessage(boolean isValid, ConstraintValidatorContext context) {
            if (isValid) {
                context.disableDefaultConstraintViolation();
            }
            context.buildConstraintViolationWithTemplate("Invalid parameter 'taxId'")
                    .addConstraintViolation();
        }
    }
}
