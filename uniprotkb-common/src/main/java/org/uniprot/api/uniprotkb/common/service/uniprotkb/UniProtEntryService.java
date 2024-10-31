package org.uniprot.api.uniprotkb.common.service.uniprotkb;

import static org.uniprot.api.common.repository.search.SolrQueryConverterUtils.*;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.request.UniProtKBRequestUtil.DASH;
import static org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverterImpl.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.*;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniProtSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverter;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Service
@Import(UniProtSolrQueryConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final UniProtQueryProcessorConfig uniProtQueryProcessorConfig;
    private final UniprotQueryRepository repository;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final UniProtKBRequestConverter uniProtKBRequestConverter;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniProtKBFacetConfig uniprotKBFacetConfig,
            SolrQueryConfig uniProtKBSolrQueryConf,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyLineageService taxService,
            FacetTupleStreamTemplate uniProtKBFacetTupleStreamTemplate,
            UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig,
            SearchFieldConfig uniProtKBSearchFieldConfig,
            TupleStreamDocumentIdStream uniProtKBTupleStreamDocumentIdStream,
            RdfStreamer uniProtRdfStreamer,
            UniProtKBRequestConverter uniProtKBRequestConverter) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtEntryStoreStreamer,
                uniProtKBSolrQueryConf,
                uniProtKBFacetTupleStreamTemplate,
                uniProtKBTupleStreamDocumentIdStream,
                uniProtKBRequestConverter);
        this.repository = repository;
        this.uniProtQueryProcessorConfig = uniProtKBQueryProcessorConfig;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.searchFieldConfig = uniProtKBSearchFieldConfig;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        this.rdfStreamer = uniProtRdfStreamer;
        this.uniProtKBRequestConverter = uniProtKBRequestConverter;
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {
        SolrRequest solrRequest = uniProtKBRequestConverter.createSearchSolrRequest(request);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<ReturnField> fields = OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        Set<ProblemPair> warnings =
                getWarnings(
                        request.getQuery(),
                        this.uniProtQueryProcessorConfig.getLeadingWildcardFields());
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return convertQueryResult(results, warnings);
        } else {
            return resultsConverter.convertQueryResult(results, fields, warnings);
        }
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(ACCESSION_ID);
    }

    public UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniProtQueryProcessorConfig;
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession, String fields) {
        try {
            List<ReturnField> fieldList = OutputFieldsParser.parse(fields, returnFieldConfig);
            accession = accession.toUpperCase();
            SolrRequest solrRequest =
                    uniProtKBRequestConverter.createAccessionSolrRequest(accession);
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            if (accession.endsWith(CANONICAL_ISOFORM) && optionalDoc.isEmpty()) {
                accession = accession.substring(0, accession.indexOf(CANONICAL_ISOFORM));
                solrRequest = uniProtKBRequestConverter.createAccessionSolrRequest(accession);
                optionalDoc = repository.getEntry(solrRequest);
            }
            Optional<UniProtKBEntry> optionalUniProtEntry =
                    optionalDoc
                            .map(doc -> resultsConverter.convertDoc(doc, fieldList))
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return optionalUniProtEntry.orElseThrow(
                    () -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    @Override
    public Stream<UniProtKBEntry> stream(StreamRequest request) {
        SolrRequest query = uniProtKBRequestConverter.createStreamSolrRequest(request);
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return this.solrIdStreamer
                    .fetchIds(query)
                    .map(this::mapToThinEntry)
                    .filter(Objects::nonNull);
        } else {
            StoreRequest storeRequest = getStoreRequest(request);
            return super.storeStreamer.idsToStoreStream(query, storeRequest);
        }
    }

    public List<FacetField> getFacets(String query, Map<String, String> facetFields) {
        SolrQuery solrQuery = new SolrQuery(query);
        facetFields.forEach(solrQuery::set);
        solrQuery.set(FacetParams.FACET, true);
        solrQuery.set(DEF_TYPE, "edismax");
        solrQuery.add(QUERY_FIELDS, uniProtKBRequestConverter.getQueryFields(query));
        if (UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION, IS_ISOFORM, query, false)) {
            solrQuery.add(FILTER_QUERY, getQueryFieldName(IS_ISOFORM) + ":" + false);
        }
        return repository.query(solrQuery).getFacetFields();
    }

    public String findAccessionByProteinId(String proteinId) {
        try {
            SolrRequest solrRequest =
                    uniProtKBRequestConverter.createProteinIdSolrRequest(proteinId);
            QueryResult<UniProtDocument> queryResult = repository.searchPage(solrRequest, null);
            if (queryResult.getPage().getTotalElements() == 0) {
                throw new ResourceNotFoundException("{search.not.found}");
            }
            List<UniProtDocument> solrDocResults =
                    queryResult.getContent().collect(Collectors.toList());

            if (solrDocResults.size() > 1) {
                solrDocResults = filterAndHandleObsoleteIds(proteinId, solrDocResults);
            }

            if (solrDocResults.size() > 1) {
                throw new ImportantMessageServiceException(
                        "Multiple accessions found for id: " + proteinId);
            }
            return solrDocResults.get(0).accession;
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get protein id for: [" + proteinId + "]";
            throw new ServiceException(message, e);
        }
    }

    private List<UniProtDocument> filterAndHandleObsoleteIds(
            String proteinId, List<UniProtDocument> docResult) {
        List<UniProtDocument> primaryIdsResults =
                docResult.stream()
                        .filter(doc -> doc.active != null && doc.active)
                        .filter(doc -> proteinId.equalsIgnoreCase(doc.id.get(0)))
                        .collect(Collectors.toList());

        if (primaryIdsResults.isEmpty()) {
            // in this case all found documents are obsolete
            String duplicatedAccessions =
                    docResult.stream()
                            .map(UniProtDocument::getDocumentId)
                            .collect(Collectors.joining(", "));

            throw new InvalidRequestException(
                    "This protein ID '"
                            + proteinId
                            + "' is now obsolete. Please refer to the accessions derived from this protein ID ("
                            + duplicatedAccessions
                            + ").");
        }
        return primaryIdsResults;
    }

    public Stream<String> streamRdf(
            UniProtKBStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest = uniProtKBRequestConverter.createStreamSolrRequest(streamRequest);
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).collect(Collectors.toList());
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    @Override
    protected Stream<UniProtKBEntry> streamEntries(
            List<String> idsInPage, IdsSearchRequest request) {
        StoreRequest storeRequest = getStoreRequest(request);
        return this.storeStreamer.streamEntries(idsInPage, storeRequest);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION_ID)
                .getFieldName();
    }

    @Override
    protected String getTermsQueryField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION)
                .getFieldName();
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected UniProtKBEntry mapToThinEntry(String accession) {
        UniProtKBEntryBuilder builder =
                new UniProtKBEntryBuilder(accession, accession, UniProtKBEntryType.SWISSPROT);
        return builder.build();
    }

    @Override
    protected boolean hasIsoformIds(List<String> ids) {
        return ids.stream().anyMatch(id -> id.contains(DASH));
    }

    @Override
    public StoreRequest getStoreRequest(BasicRequest request) {
        List<ReturnField> fieldList =
                OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        StoreRequest.StoreRequestBuilder storeRequest = StoreRequest.builder();
        storeRequest.fields(request.getFields());
        if (resultsConverter.hasLineage(fieldList)) {
            storeRequest.addLineage(true);
        }
        return storeRequest.build();
    }

    public String getQueryFieldName(String active) {
        return searchFieldConfig.getSearchFieldItemByName(active).getFieldName();
    }

    private QueryResult<UniProtKBEntry> convertQueryResult(
            QueryResult<UniProtDocument> results, Set<ProblemPair> warnings) {
        Stream<UniProtKBEntry> upEntries =
                results.getContent()
                        .map(Document::getDocumentId)
                        .map(this::mapToThinEntry)
                        .filter(Objects::nonNull);
        return QueryResult.<UniProtKBEntry>builder()
                .content(upEntries)
                .page(results.getPage())
                .facets(results.getFacets())
                .matchedFields(results.getMatchedFields())
                .suggestions(results.getSuggestions())
                .warnings(warnings)
                .build();
    }
}
