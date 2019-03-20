package uk.ac.ebi.uniprot.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.common.repository.search.facet.GenericFacetConfig;

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
 *
 * WHEN THE VALUE IS TRUE, it must also verify if the accept content type is "application/json"
 * because facets are only supported by json format requests.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidFacets.ValidIncludeFacetsValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFacets {

    Class<? extends GenericFacetConfig> facetConfig();

    String message() default "{search.invalid.includeFacet}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    public class ValidIncludeFacetsValidator implements ConstraintValidator<ValidFacets, String> {

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
                GenericFacetConfig facetConfig = applicationContext.getBean(constraintAnnotation.facetConfig());
                facetNames = facetConfig.getFacetNames();
            } catch (Exception e) {
                LOGGER.error("Unable to instanciate facet connfig", e);
                facetNames = new ArrayList<>();
            }
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if(Utils.notEmpty(value)){
                //first validate if the accept is for application/json
                ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
                String accept = getRequest().getHeader("Accept");
                if(accept == null || !accept.equalsIgnoreCase("application/json")){
                    buildUnsuportedContentTypeErrorMessage(accept,contextImpl);
                    isValid = false;
                }else {
                    // verify if facet name is valid.
                    String[] facetList = value.split("\\s*,\\s*");
                    for (String facet : facetList) {
                        if(!getFacetNames().contains(facet)){
                            buildInvalidFacetNameMessage(facet,getFacetNames(),contextImpl);
                            isValid = false;
                        }
                    }
                }
                if(!isValid && contextImpl != null){
                    contextImpl.disableDefaultConstraintViolation();
                }
            }
            return isValid;
        }

        HttpServletRequest getRequest() {
            return request;
        }

        Collection<String> getFacetNames(){
            return facetNames;
        }

        void buildUnsuportedContentTypeErrorMessage(String contentType, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.includeFacet.content.type}";
            contextImpl.addMessageParameter("contentType",contentType);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
        void buildInvalidFacetNameMessage(String facetName,Collection<String> validNames, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.facet.name}";
            contextImpl.addMessageParameter("facetName",facetName);
            contextImpl.addMessageParameter("validNames", "["+String.join(", ",validNames)+"]");
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
