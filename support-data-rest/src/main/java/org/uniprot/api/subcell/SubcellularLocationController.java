package org.uniprot.api.subcell;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.SUBCELLULAR_LOCATION;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.subcell.request.SubcellularLocationRequestDTO;
import org.uniprot.api.subcell.service.SubcellularLocationService;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@RestController
@RequestMapping("/subcellularlocation")
@Validated
public class SubcellularLocationController extends BasicSearchController<SubcellularLocationEntry> {

    private final SubcellularLocationService subcellularLocationService;
    private static final String SUBCELLULAR_LOCATION_ID_REGEX = "^SL-[0-9]{4}";

    public SubcellularLocationController(
            ApplicationEventPublisher eventPublisher,
            SubcellularLocationService subcellularLocationService,
            @Qualifier("subcellularLocationMessageConverterContextFactory")
                    MessageConverterContextFactory<SubcellularLocationEntry>
                            subcellularLocationMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(
                eventPublisher,
                subcellularLocationMessageConverterContextFactory,
                downloadTaskExecutor,
                SUBCELLULAR_LOCATION);
        this.subcellularLocationService = subcellularLocationService;
    }

    @GetMapping(
            value = "/{subcellularLocationId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<SubcellularLocationEntry>> getById(
            @PathVariable("subcellularLocationId")
                    @Pattern(
                            regexp = SUBCELLULAR_LOCATION_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.subcellularLocation.invalid.id}")
                    String subcellularLocationId,
            @ValidReturnFields(fieldValidatorClazz = SubcellularLocationField.ResultFields.class)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        MediaType contentType = getAcceptHeader(request);
        SubcellularLocationEntry subcellularLocationEntry =
                this.subcellularLocationService.findByUniqueId(subcellularLocationId);
        return super.getEntityResponse(subcellularLocationEntry, fields, contentType, request);
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<SubcellularLocationEntry>> search(
            @Valid SubcellularLocationRequestDTO searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        MediaType contentType = getAcceptHeader(request);
        QueryResult<SubcellularLocationEntry> results =
                subcellularLocationService.search(searchRequest);
        return super.getSearchResponse(
                results, searchRequest.getFields(), contentType, request, response);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                OBO_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<SubcellularLocationEntry>>>
            download(
                    @Valid SubcellularLocationRequestDTO searchRequest,
                    @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                            MediaType contentType,
                    @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
                    HttpServletRequest request) {
        Stream<SubcellularLocationEntry> result =
                subcellularLocationService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(SubcellularLocationEntry entity) {
        return entity.getAccession();
    }

    @Override
    protected Optional<String> getEntityRedirectId(SubcellularLocationEntry entity) {
        return Optional.empty();
    }
}
