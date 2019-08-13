package org.uniprot.api.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.core.util.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This Include Facets Constraint Validator class is responsible to verify if the parameter value is true or false,
 * <p>
 * WHEN THE VALUE IS TRUE, it must also verify if the accept content type is "application/json"
 * because facets are only supported by json format requests.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidFacets.ValidIncludeFacetsValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFacets {
    Class<? extends FacetConfig> facetConfig();

    String message() default "{search.invalid.includeFacet}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidIncludeFacetsValidator implements ConstraintValidator<ValidFacets, String> {
        private static final Logger LOGGER = LoggerFactory.getLogger(ValidIncludeFacetsValidator.class);

        @Autowired
        private HttpServletRequest request;

        @Autowired
        private ApplicationContext applicationContext;

        private Collection<String> facetNames;

        @Override
        public void initialize(ValidFacets constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            try {
                FacetConfig facetConfig = applicationContext.getBean(constraintAnnotation.facetConfig());
                facetNames = facetConfig.getFacetNames();
            } catch (Exception e) {
                LOGGER.error("Unable to instantiate facet config", e);
                facetNames = new ArrayList<>();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if (Utils.notEmpty(value)) {
                // verify if facet name is valid.
                ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
                String[] facetList = value.split("\\s*,\\s*");
                for (String facet : facetList) {
                    if (!getFacetNames().contains(facet)) {
                        buildInvalidFacetNameMessage(facet, getFacetNames(), contextImpl);
                        isValid = false;
                    }
                }

                if (!isValid && contextImpl != null) {
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        HttpServletRequest getRequest() {
            return request;
        }

        Collection<String> getFacetNames() {
            return facetNames;
        }

        void buildInvalidFacetNameMessage(String facetName, Collection<String> validNames, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.facet.name}";
            contextImpl.addMessageParameter("0", facetName);
            contextImpl.addMessageParameter("1", "[" + String.join(", ", validNames) + "]");
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
