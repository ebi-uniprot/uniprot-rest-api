package org.uniprot.api.unirule.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIRULE;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.unirule.service.UniRuleService;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@RestController
@Validated
@RequestMapping("/unirule")
public class UniRuleController extends BasicSearchController<UniRuleEntry> {

    private final UniRuleService uniRuleService;
    public static final String UNIRULE_ID_REGEX = "UR(\\d{9})";

    @Autowired
    public UniRuleController(
            ApplicationEventPublisher eventPublisher,
            MessageConverterContextFactory<UniRuleEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor,
            UniRuleService uniRuleService) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIRULE);
        this.uniRuleService = uniRuleService;
    }

    @GetMapping(
            value = "/{uniruleid}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<UniRuleEntry>> getByUniRuleId(
            @PathVariable("uniruleid")
                    @Pattern(
                            regexp = UNIRULE_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.unirule.invalid.id}")
                    String uniRuleId,
            @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE) String fields,
            HttpServletRequest request) {
        UniRuleEntry entryResult = this.uniRuleService.findByUniqueId(uniRuleId);
        return super.getEntityResponse(entryResult, fields, request);
    }

    @Override
    protected String getEntityId(UniRuleEntry entity) {
        return entity.getUniRuleId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniRuleEntry entity) {
        return Optional.empty();
    }
}
