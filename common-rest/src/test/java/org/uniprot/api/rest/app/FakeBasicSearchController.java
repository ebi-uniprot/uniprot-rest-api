package org.uniprot.api.rest.app;

import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.common.concurrency.Gatekeeper;
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

    @Autowired
    public FakeBasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<String> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.TAXONOMY);
    }

    private FakeBasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<String> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        super(
                eventPublisher,
                converterContextFactory,
                downloadTaskExecutor,
                MessageConverterContextFactory.Resource.TAXONOMY,
                downloadGatekeeper);
    }

    protected FakeBasicSearchController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<String> converterContextFactory) {
        super(
                eventPublisher,
                converterContextFactory,
                null,
                MessageConverterContextFactory.Resource.TAXONOMY);
    }

    public static FakeBasicSearchController createInstance(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<String> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            Gatekeeper downloadGatekeeper) {
        return new FakeBasicSearchController(
                eventPublisher, converterContextFactory, downloadTaskExecutor, downloadGatekeeper);
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
    protected Optional<String> getEntityRedirectId(String entity, HttpServletRequest request) {
        return Optional.empty();
    }
}
