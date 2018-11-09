package uk.ac.ebi.uniprot.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Include Facets Constraint Validator class is responsible to verify if the parameter value is true or false,
 *
 * WHEN THE VALUE IS TRUE, it must also verify if the accept content type is "application/json"
 * because facets are only supported by json format requests.
 *
 * @author lgonzales
 */
@Constraint(validatedBy = ValidIncludeFacets.ValidIncludeFacetsValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIncludeFacets {

    String message() default "{search.invalid.includeFacet}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    public class ValidIncludeFacetsValidator implements ConstraintValidator<ValidIncludeFacets, String> {

        @Autowired
        private HttpServletRequest request;

        @Override
        public void initialize(ValidIncludeFacets constraintAnnotation) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            boolean isValid = true;
            if(value != null && !value.isEmpty()){
                ConstraintValidatorContextImpl contextImpl = (ConstraintValidatorContextImpl) context;
                if(value.matches("^(?i:true|false)$")){
                    if(value.equalsIgnoreCase("true")){
                        String accept = getRequest().getHeader("Accept");
                        System.out.println("LEO ACCEPT:"+accept);
                        if(accept == null || !accept.equalsIgnoreCase("application/json")){
                            buildUnsuportedContentTypeErrorMessage(accept,contextImpl);
                            isValid = false;
                        }
                    }
                }else{
                    buildInvalidFormaErrorMessage(value,contextImpl);
                    isValid = false;
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

        void buildUnsuportedContentTypeErrorMessage(String contentType, ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.includeFacet.content.type}";
            contextImpl.addMessageParameter("contentType",contentType);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
        void buildInvalidFormaErrorMessage(String value,ConstraintValidatorContextImpl contextImpl) {
            String errorMessage = "{search.invalid.includeFacet}";
            contextImpl.addMessageParameter("value",value);
            contextImpl.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }
    }
}
