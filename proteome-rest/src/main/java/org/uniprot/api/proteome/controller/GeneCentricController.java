package org.uniprot.api.proteome.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.GENECENTRIC;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.proteome.request.GeneCentricRequest;
import org.uniprot.api.proteome.service.GeneCentricService;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.core.xml.jaxb.proteome.CanonicalGene;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@RestController
@Validated
@RequestMapping("/genecentric")
public class GeneCentricController extends BasicSearchController<CanonicalProtein> {

    private final GeneCentricService service;

    @Autowired
    public GeneCentricController(
            ApplicationEventPublisher eventPublisher,
            GeneCentricService service,
            @Qualifier("GENECENTRIC")
                    MessageConverterContextFactory<CanonicalProtein> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, GENECENTRIC);
        this.service = service;
    }

    @Tag(name = "genecentric", description = "gene centric service")
    @Operation(
            summary = "Search for gene centric data set.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalProtein
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalGene.class,
                                                                    name = "entries"))),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> searchCursor(
            @Valid @ModelAttribute GeneCentricRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<CanonicalProtein> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "genecentric")
    @RequestMapping(
            value = "/upid/{upid}",
            method = RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    @Operation(
            summary = "Fetch all proteins of Proteome id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalProtein
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalGene.class,
                                                                    name = "entries"))),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByUpId(
            @Parameter(description = "Unique identifier for the Proteome entry")
                    @PathVariable("upid")
                    @Pattern(
                            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.upid.value}")
                    String upid,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "genecentric-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request,
            HttpServletResponse response) {
        GeneCentricRequest searchRequest = new GeneCentricRequest();
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.GENECENTRIC);
        String query =
                searchFieldConfig.getSearchFieldItemByName("upid").getFieldName() + ":" + upid;
        searchRequest.setQuery(query);
        searchRequest.setFields(fields);
        QueryResult<CanonicalProtein> results = service.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @Tag(name = "genecentric")
    @Operation(
            summary = "Download Gene Centric data retrieved by search.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalProtein
                                                                                    .class))),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema =
                                                            @Schema(
                                                                    implementation =
                                                                            CanonicalGene.class,
                                                                    name = "entries"))),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {LIST_MEDIA_TYPE_VALUE, APPLICATION_JSON_VALUE, XLS_MEDIA_TYPE_VALUE})
    public DeferredResult<ResponseEntity<MessageConverterContext<CanonicalProtein>>> download(
            @Valid @ModelAttribute GeneCentricRequest searchRequest,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        Stream<CanonicalProtein> result = service.download(searchRequest);
        return super.download(
                result, searchRequest.getFields(), getAcceptHeader(request), request, encoding);
    }

    @Tag(name = "genecentric")
    @Operation(
            summary = "Retrieve an gene centric entry by uniprot accession.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CanonicalProtein.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = CanonicalGene.class)),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE)
                        })
            })
    @RequestMapping(
            value = "/{accession}",
            method = RequestMethod.GET,
            produces = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, LIST_MEDIA_TYPE_VALUE})
    public ResponseEntity<MessageConverterContext<CanonicalProtein>> getByAccession(
            @Parameter(description = "UnirotKB accession")
                    @PathVariable("accession")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.accession.value}")
                    String accession,
            @ModelFieldMeta(
                            reader = ReturnFieldMetaReaderImpl.class,
                            path = "genecentric-return-fields.json")
                    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        CanonicalProtein entry = service.findByUniqueId(accession.toUpperCase());
        return super.getEntityResponse(entry, fields, request);
    }

    @Override
    protected String getEntityId(CanonicalProtein entity) {
        return entity.getCanonicalProtein().getAccession().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(CanonicalProtein entity) {
        return Optional.empty();
    }
}
