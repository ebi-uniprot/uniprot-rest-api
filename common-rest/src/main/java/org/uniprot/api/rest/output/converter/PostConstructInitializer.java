package org.uniprot.api.rest.output.converter;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Component
public class PostConstructInitializer {
    @Autowired private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @PostConstruct
    public void init() {
        // inject custom handler after the initialisation
        List<HandlerMethodReturnValueHandler> handlers =
                new ArrayList<>(this.requestMappingHandlerAdapter.getReturnValueHandlers());
        List<HttpMessageConverter<?>> converters =
                this.requestMappingHandlerAdapter.getMessageConverters();
        CustomResponseBodyEmitterReturnValueHandler customHandler =
                new CustomResponseBodyEmitterReturnValueHandler(converters);
        handlers.add(0, customHandler); // add in the beginning to match accurately
        this.requestMappingHandlerAdapter.setReturnValueHandlers(handlers);
    }
}
