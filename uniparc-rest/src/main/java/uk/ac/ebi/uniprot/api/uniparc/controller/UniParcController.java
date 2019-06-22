package uk.ac.ebi.uniprot.api.uniparc.controller;

import static uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIPARC;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import uk.ac.ebi.uniprot.api.rest.controller.BasicSearchController;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.api.uniparc.service.UniParcQueryService;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;

/**
 *
 * @author jluo
 * @date: 21 Jun 2019
 *
*/
@RestController
@Api(tags = { "uniparc" })
@Validated
@RequestMapping("/uniparc")
public class UniParcController extends BasicSearchController<UniParcEntry> {

	private final UniParcQueryService queryService;

	@Autowired
	public UniParcController(ApplicationEventPublisher eventPublisher, UniParcQueryService queryService,
			 MessageConverterContextFactory<UniParcEntry> converterContextFactory,
			ThreadPoolTaskExecutor downloadTaskExecutor) {
		super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIPARC);
		this.queryService = queryService;
	}

	@Override
	protected String getEntityId(UniParcEntry entity) {
		return entity.getUniParcId().getValue();
	}

	@Override
	protected Optional<String> getEntityRedirectId(UniParcEntry entity) {
		return Optional.empty();
	}

}

