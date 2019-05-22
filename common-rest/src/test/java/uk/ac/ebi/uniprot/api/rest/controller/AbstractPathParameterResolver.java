package uk.ac.ebi.uniprot.api.rest.controller;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
/**
 *
 * @author lgonzales
 */
public abstract class AbstractPathParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(PathParameter.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        PathParameter result = null;
        Method method = extensionContext.getTestMethod().orElseThrow(() -> new RuntimeException("AbstractContentTypeRequestParam: Unable to find tested method"));
        switch (method.getName()) {
            case "getIdCanReturnSuccessContentType":
            case "getIdCanFilterFieldsEntryReturnSuccess":
                result = getSuccessPathParameter();
                break;
            case "getIdCanReturnBadRequestContentType":
                result = getBadRequestPathParameter();
                break;
            case "getIdCanReturnNotFoundRequest":
                result = getNotFoundPathParameter();
                break;
        }
        return result;
    }

    public abstract PathParameter getSuccessPathParameter();

    public abstract PathParameter getBadRequestPathParameter();

    public abstract PathParameter getNotFoundPathParameter();

}
