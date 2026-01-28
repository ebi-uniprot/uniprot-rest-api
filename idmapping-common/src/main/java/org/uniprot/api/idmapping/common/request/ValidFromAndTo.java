package org.uniprot.api.idmapping.common.request;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 01/03/2021
 *
 * @author sahmad
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

    @Slf4j
    class ValidFromAndToValidator implements ConstraintValidator<ValidFromAndTo, Object> {
        private String fromFieldName;
        private String toFieldName;
        private List<UniProtDatabaseDetail> dbDetails;

        private static final Set<String> UNIPROT_GROUP_TYPES =
                Set.of(ACC_ID_STR, UPARC_STR, UNIREF50_STR, UNIREF90_STR, UNIREF100_STR);
        private static final Set<String> UNIPROT_GROUP_TYPES_MINUS_UNIPROTKB =
                Set.of(UPARC_STR, UNIREF50_STR, UNIREF90_STR, UNIREF100_STR);

        @Override
        public void initialize(ValidFromAndTo constraintAnnotation) {
            this.fromFieldName = constraintAnnotation.from();
            this.toFieldName = constraintAnnotation.to();
            this.dbDetails = IdMappingFieldConfig.getAllIdMappingTypes();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            boolean isValid;

            try {
                String fromValue = BeanUtils.getProperty(value, this.fromFieldName);
                String toValue = BeanUtils.getProperty(value, this.toFieldName);

                // SwissProt type cannot be in from type
                boolean isValidFrom =
                        this.dbDetails.stream()
                                .anyMatch(
                                        db ->
                                                db.getName().equals(fromValue)
                                                        && !SWISSPROT_STR.equals(fromValue));

                isValid = isValidFrom && isValidPair(fromValue, toValue);
                if (!isValid) {
                    ConstraintValidatorContextImpl contextImpl =
                            (ConstraintValidatorContextImpl) context;
                    contextImpl.addMessageParameter("0", fromValue);
                    contextImpl.addMessageParameter("1", toValue);
                }
            } catch (Exception e) {
                log.warn("Error during validation {}", e.getMessage());
                isValid = false;
            }
            return isValid;
        }

        private boolean isValidPair(String from, String to) {
            boolean isValid = true;

            // Non-UniProt from types (e.g. PIR, PDB etc) can only be mapped to either Trembl or
            // Swissprot type
            if (!UNIPROT_GROUP_TYPES.contains(from)) {
                isValid = isUniProtKBOrSwissProt(to);
            }

            if (PROTEOME_STR.equals(from)) {
                isValid = isUniProtKBOrSwissProtOrUniParc(to);
            }

            // From types (except UniProtKB AC/ID) of UniProt group can be mapped to Trembl,
            // SwissProt or self type
            if (UNIPROT_GROUP_TYPES_MINUS_UNIPROTKB.contains(from)) {
                isValid = isUniProtKBOrSwissProt(to) || from.equals(to);
            }

            // From type 'UniProtKB AC/ID' can mapped to all possible to type except self
            if (ACC_ID_STR.equals(from)) {
                isValid =
                        this.dbDetails.stream()
                                .anyMatch(db -> db.getName().equals(to) && !ACC_ID_STR.equals(to));
            }

            return isValid;
        }

        private boolean isUniProtKBOrSwissProt(String to) {
            return IdMappingFieldConfig.ACC_STR.equals(to)
                    || IdMappingFieldConfig.SWISSPROT_STR.equals(to);
        }

        private boolean isUniProtKBOrSwissProtOrUniParc(String to) {
            return isUniProtKBOrSwissProt(to) || UNIPARC_STR.equals(to);
        }
    }
}
