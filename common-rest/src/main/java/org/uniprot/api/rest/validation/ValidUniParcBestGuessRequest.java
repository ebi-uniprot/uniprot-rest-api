package org.uniprot.api.rest.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.beanutils.BeanUtils;
import org.uniprot.core.util.Utils;

@Constraint(validatedBy = ValidUniParcBestGuessRequest.UniParcBestGuessRequestValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUniParcBestGuessRequest {
    String message() default "{uniparc.best.guess.invalid}";

    String upis() default "upis";

    String accessions() default "accessions";

    String dbids() default "dbids";

    String genes() default "genes";

    String taxonIds() default "taxonIds";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UniParcBestGuessRequestValidator
            implements javax.validation.ConstraintValidator<ValidUniParcBestGuessRequest, Object> {
        private String upis;
        private String accessions;
        private String dbids;
        private String genes;
        private String taxonIds;

        @Override
        public void initialize(ValidUniParcBestGuessRequest constraintAnnotation) {
            this.upis = constraintAnnotation.upis();
            this.accessions = constraintAnnotation.accessions();
            this.dbids = constraintAnnotation.dbids();
            this.genes = constraintAnnotation.genes();
            this.taxonIds = constraintAnnotation.taxonIds();
        }

        @Override
        public boolean isValid(Object value, javax.validation.ConstraintValidatorContext context) {
            boolean isValid = true;
            try {
                String upisValue = BeanUtils.getProperty(value, upis);
                String accessionsValue = BeanUtils.getProperty(value, accessions);
                String dbidsValue = BeanUtils.getProperty(value, dbids);
                String genesValue = BeanUtils.getProperty(value, genes);
                String taxonIdsValue = BeanUtils.getProperty(value, taxonIds);

                boolean upisNotEmpty = !Utils.nullOrEmpty(upisValue);
                boolean accessionsNotEmpty = !Utils.nullOrEmpty(accessionsValue);
                boolean dbidsNotEmpty = !Utils.nullOrEmpty(dbidsValue);
                boolean genesNotEmpty = !Utils.nullOrEmpty(genesValue);
                boolean taxonIdsNotEmpty = !Utils.nullOrEmpty(taxonIdsValue);

                if (upisNotEmpty
                        || accessionsNotEmpty
                        || dbidsNotEmpty
                        || genesNotEmpty
                        || taxonIdsNotEmpty) {
                    if (dbidsNotEmpty) {
                        isValid =
                                upisNotEmpty
                                        || accessionsNotEmpty
                                        || genesNotEmpty
                                        || taxonIdsNotEmpty;
                    }
                } else {
                    isValid = false;
                }
            } catch (Exception e) {
                isValid = false;
            }
            if (!isValid) {
                String errorMessage = "{uniparc.best.guess.invalid}";
                buildErrorMessage(context, errorMessage);
            }
            return isValid;
        }

        void buildErrorMessage(ConstraintValidatorContext context, String message) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
    }
}
