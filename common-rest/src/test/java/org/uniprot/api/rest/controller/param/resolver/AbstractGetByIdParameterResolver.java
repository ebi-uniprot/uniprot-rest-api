package org.uniprot.api.rest.controller.param.resolver;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.uniprot.api.rest.controller.param.GetIdParameter;

/**
 * @author lgonzales
 */
public abstract class AbstractGetByIdParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(GetIdParameter.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        GetIdParameter result = null;
        Method method =
                extensionContext
                        .getTestMethod()
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "AbstractGetByIdParameterResolver: Unable to find tested method"));
        switch (method.getName()) {
            case "validIdReturnSuccess":
            case "idWithoutContentTypeMeansUseDefaultContentType":
            case "idWithExtensionMeansUseThatContentType":
            case "idWithInvalidExtensionMeansBadRequestInDefaultContentType":
                result = validIdParameter();
                break;
            case "invalidIdReturnBadRequest":
                result = invalidIdParameter();
                break;
            case "nonExistentIdReturnFoundRequest":
                result = nonExistentIdParameter();
                break;
            case "withFilterFieldsReturnSuccess":
                result = withFilterFieldsParameter();
                break;
            case "withInvalidFilterFieldsReturnBadRequest":
                result = withInvalidFilterParameter();
                break;
        }
        return result;
    }

    protected abstract GetIdParameter validIdParameter();

    protected abstract GetIdParameter invalidIdParameter();

    protected abstract GetIdParameter nonExistentIdParameter();

    protected abstract GetIdParameter withFilterFieldsParameter();

    protected abstract GetIdParameter withInvalidFilterParameter();
}
