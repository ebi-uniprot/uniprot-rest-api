package org.uniprot.api.literature;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.literature.request.LiteratureMappedRequestDTO;
import org.uniprot.api.literature.request.LiteratureRequestDTO;
import org.uniprot.api.literature.service.LiteratureService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.field.LiteratureField;
import org.uniprot.store.search.field.validator.FieldValueValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.LITERATURE;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@RestController
@RequestMapping("/literature")
@Validated
public class LiteratureController extends BasicSearchController<LiteratureEntry> {

    private final LiteratureService literatureService;
    private static final String LITERATURE_ID_REGEX = "^[0-9]+$";

    public LiteratureController(ApplicationEventPublisher eventPublisher, LiteratureService literatureService,
                                @Qualifier("literatureMessageConverterContextFactory") MessageConverterContextFactory<LiteratureEntry> literatureMessageConverterContextFactory,
                                ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, literatureMessageConverterContextFactory, downloadTaskExecutor, LITERATURE);
        this.literatureService = literatureService;
    }

    @GetMapping(value = "/{literatureId}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> getByLiteratureId(@PathVariable("literatureId")
                                                                            @Pattern(regexp = LITERATURE_ID_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.literature.invalid.id}")
                                                                                    String literatureId,
                                                                                      @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
                                                                            @RequestParam(value = "fields", required = false)
                                                                                    String fields,
                                                                                      @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                    MediaType contentType) {
        String updatedFields = updateFieldsWithoutMappedReferences(fields, contentType);
        LiteratureEntry literatureEntry = this.literatureService.findById(literatureId);
        return super.getEntityResponse(literatureEntry, updatedFields, contentType);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> search(@Valid
                                                                                   LiteratureRequestDTO searchRequest,
                                                                           @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                   MediaType contentType,
                                                                           HttpServletRequest request,
                                                                           HttpServletResponse response) {
        String updatedFields = updateFieldsWithoutMappedReferences(searchRequest.getFields(), contentType);
        QueryResult<LiteratureEntry> results = literatureService.search(searchRequest);
        return super.getSearchResponse(results, updatedFields, contentType, request, response);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET,
            produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<ResponseBodyEmitter> download(@Valid
                                                                LiteratureRequestDTO searchRequest,
                                                        @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                MediaType contentType,
                                                        @RequestHeader(value = "Accept-Encoding", required = false)
                                                                String encoding,
                                                        HttpServletRequest request) {
        Stream<LiteratureEntry> result = literatureService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @GetMapping(value = "/mapped/proteins/{accession}", produces = {TSV_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<LiteratureEntry>> getMappedLiteratureByUniprotAccession(@PathVariable("accession")
                                                                                                          @Pattern(regexp = FieldValueValidator.ACCESSION_REGEX, flags = {Pattern.Flag.CASE_INSENSITIVE}, message = "{search.literature.invalid.accession}")
                                                                                                                  String accession,
                                                                                                          @Valid
                                                                                                                  LiteratureMappedRequestDTO requestDTO,
                                                                                                          @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                                                                                                                  MediaType contentType,
                                                                                                          HttpServletRequest request,
                                                                                                          HttpServletResponse response) {
        QueryResult<LiteratureEntry> literatureEntry = this.literatureService.getMappedLiteratureByUniprotAccession(accession, requestDTO);
        return super.getSearchResponse(literatureEntry, requestDTO.getFields(), contentType, request, response);
    }

    @Override
    protected String getEntityId(LiteratureEntry entity) {
        return String.valueOf(entity.getPubmedId());
    }

    @Override
    protected Optional<String> getEntityRedirectId(LiteratureEntry entity) {
        return Optional.empty();
    }

    private String updateFieldsWithoutMappedReferences(String requestField, MediaType contentType) {
        if (Utils.nullOrEmpty(requestField) && contentType.equals(APPLICATION_JSON)) {
            requestField = Arrays.stream(LiteratureField.ResultFields.values())
                    .filter(resultFields -> !resultFields.name().equals(LiteratureField.ResultFields.mapped_references.name()))
                    .map(LiteratureField.ResultFields::name)
                    .collect(Collectors.joining(", "));
        }
        return requestField;
    }

}
