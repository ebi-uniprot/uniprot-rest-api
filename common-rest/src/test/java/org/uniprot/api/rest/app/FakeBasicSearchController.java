package org.uniprot.api.rest.app;

import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

/**
 * @author lgonzales
 * @since 29/07/2020
 */
@Profile("use-fake-app")
@RestController
@RequestMapping("/fakeBasicSearch")
public class FakeBasicSearchController extends BasicSearchController<String> {

    protected FakeBasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<String> converterContextFactory) {
        super(
                eventPublisher,
                converterContextFactory,
                null,
                MessageConverterContextFactory.Resource.TAXONOMY);
    }

    @GetMapping(value = "/fakeId", produces = LIST_MEDIA_TYPE_VALUE)
    public ResponseEntity<MessageConverterContext<String>> getByAccession(
            HttpServletRequest request) {
        return super.getEntityResponse("Response value", "", request);
    }

    @Override
    protected String getEntityId(String entity) {
        return entity;
    }

    @Override
    protected Optional<String> getEntityRedirectId(String entity) {
        return Optional.empty();
    }
}
