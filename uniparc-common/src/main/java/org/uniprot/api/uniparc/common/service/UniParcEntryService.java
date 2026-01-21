package org.uniprot.api.uniparc.common.service;

import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniParcSolrQueryConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.service.filter.UniParcCrossReferenceTaxonomyFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcProteomeIdFilter;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.request.*;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.util.MessageDigestUtil;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
@Import(UniParcSolrQueryConfig.class)
public class UniParcEntryService extends StoreStreamerSearchService<UniParcDocument, UniParcEntry> {
    public static final String UNIPARC_ID_FIELD = "upi";
    private static final String ACCESSION_FIELD = "uniprotkb";
    public static final String CHECKSUM_STR = "checksum";
    private static final String COMMA_STR = ",";
    private final SearchFieldConfig searchFieldConfig;
    private final UniParcQueryRepository repository;
    private final UniParcLightStoreClient uniParcLightStoreClient;
    private final UniParcCrossReferenceService uniParcCrossReferenceService;
    private final RdfStreamer rdfStreamer;

    @Autowired
    public UniParcEntryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            SolrQueryConfig uniParcSolrQueryConf,
            SearchFieldConfig uniParcSearchFieldConfig,
            RdfStreamer uniParcRdfStreamer,
            FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate,
            TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream,
            UniParcLightStoreClient uniParcLightStoreClient,
            UniParcCrossReferenceService uniParcCrossReferenceService,
            RequestConverter uniParcRequestConverter,
            StoreStreamer<UniParcEntry> uniParcFastaStoreStreamer) {

        super(
                repository,
                facetConfig,
                uniParcFastaStoreStreamer,
                uniParcSolrQueryConf,
                uniParcFacetTupleStreamTemplate,
                uniParcTupleStreamDocumentIdStream,
                uniParcRequestConverter);
        this.searchFieldConfig = uniParcSearchFieldConfig;
        this.repository = repository;
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
        this.rdfStreamer = uniParcRdfStreamer;
    }

    public UniParcEntry getByUniParcId(UniParcGetByUniParcIdRequest uniParcIdRequest) {
        return getUniParcEntry(uniParcIdRequest.getUpi(), uniParcIdRequest);
    }

    public UniParcEntry getByUniProtAccession(UniParcGetByAccessionRequest getByAccessionRequest) {
        String uniParcId = searchUniParcId(ACCESSION_FIELD, getByAccessionRequest.getAccession());
        return getUniParcEntry(uniParcId, getByAccessionRequest);
    }

    public UniParcEntry getBySequence(UniParcSequenceRequest sequenceRequest) {
        String md5Value = MessageDigestUtil.getMD5(sequenceRequest.getSequence());
        String uniParcId = searchUniParcId(CHECKSUM_STR, md5Value);
        return getUniParcEntry(uniParcId, sequenceRequest);
    }

    public QueryResult<UniParcEntry> searchByProteomeId(
            UniParcGetByProteomeIdRequest searchRequest, String proteomeId) {
        // search uniparc entries from solr
        SolrRequest solrRequest = getRequestConverter().createSearchSolrRequest(searchRequest);
        QueryResult<UniParcDocument> results =
                repository.searchPage(solrRequest, searchRequest.getCursor());

        // convert solr docs to entries
        Stream<UniParcEntry> entries =
                results.getContent()
                        .map(d -> getUniParcEntry(d.getDocumentId(), searchRequest, proteomeId));
        return QueryResult.<UniParcEntry>builder()
                .content(entries)
                .page(results.getPage())
                .facets(results.getFacets())
                .build();
    }

    public Stream<UniParcEntry> streamByProteomeId(StreamRequest streamRequest, String proteomeId) {
        SolrRequest solrRequest = getRequestConverter().createStreamSolrRequest(streamRequest);
        StoreRequest storeRequest = StoreRequest.builder().proteomeId(proteomeId).build();
        return this.storeStreamer.idsToStoreStream(solrRequest, storeRequest);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD);
    }

    @Override
    public UniParcEntry findByUniqueId(String uniqueId, String filters) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                .getSearchFieldItemByName(UNIPARC_ID_FIELD)
                .getFieldName();
    }

    @Override
    protected UniParcEntry mapToThinEntry(String uniParcId) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcId);
        return builder.build();
    }

    @Override
    public QueryResult<UniParcEntry> search(SearchRequest request) {
        throw new UnsupportedOperationException("Operation not supported. Use light service");
    }

    @Override
    public Stream<UniParcEntry> stream(StreamRequest request) {
        throw new UnsupportedOperationException("Operation not supported. Use light service");
    }

    private UniParcEntry getUniParcEntry(String uniParcId, UniParcGetByIdRequest request) {
        return getUniParcEntry(uniParcId, request, null);
    }

    private UniParcEntry getUniParcEntry(
            String uniParcId, UniParcGetByIdRequest request, String proteomeId) {
        Optional<UniParcEntryLight> optLightEntry =
                this.uniParcLightStoreClient.getEntry(uniParcId);
        if (optLightEntry.isEmpty()) {
            throw new ResourceNotFoundException("Unable to find UniParc by id " + uniParcId);
        }
        // populate cross-references from its own store
        Stream<UniParcCrossReference> crossReferences =
                this.uniParcCrossReferenceService.getCrossReferences(
                        optLightEntry.get(), proteomeId != null);
        crossReferences = filterUniParcCrossReferenceStream(crossReferences, request, proteomeId);
        return getUniParcEntry(optLightEntry.get(), crossReferences.toList());
    }

    public UniParcEntry getUniParcEntry(String uniParcId, String crossReferenceId) {
        UniParcEntryLight uniParcLight =
                this.uniParcLightStoreClient
                        .getEntry(uniParcId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Unable to find UniParc by id " + uniParcId));

        List<UniParcCrossReference> crossReferences =
                this.uniParcCrossReferenceService
                        .getCrossReferences(uniParcLight, true)
                        .filter(cr -> crossReferenceId.equals(cr.getId()))
                        .toList();
        if (crossReferences.size() != 1) {
            UniParcCrossReference crossReference =
                    crossReferences.stream()
                            .max(Comparator.comparing(UniParcCrossReference::getLastUpdated))
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Unable to find UniParc Cross Reference by id "
                                                            + crossReferenceId));
            crossReferences = List.of(crossReference);
        }
        return getUniParcEntry(uniParcLight, crossReferences);
    }

    private UniParcEntry getUniParcEntry(
            UniParcEntryLight uniParcLight, List<UniParcCrossReference> crossReferences) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcLight.getUniParcId()).sequence(uniParcLight.getSequence());
        builder.sequenceFeaturesSet(uniParcLight.getSequenceFeatures());
        builder.uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }

    private Stream<UniParcCrossReference> filterUniParcCrossReferenceStream(
            Stream<UniParcCrossReference> uniParcCrossReferenceStream,
            UniParcGetByIdRequest request,
            String proteomeId) {
        // convert comma separated values to list
        List<String> databases = csvToList(request.getDbTypes());
        List<String> taxonomyIds = csvToList(request.getTaxonIds());
        // converters
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcCrossReferenceTaxonomyFilter taxonFilter = new UniParcCrossReferenceTaxonomyFilter();
        UniParcDatabaseStatusFilter statusFilter = new UniParcDatabaseStatusFilter();
        UniParcProteomeIdFilter proteomeIdFilter = new UniParcProteomeIdFilter();

        // filter the results
        return uniParcCrossReferenceStream
                .filter(xref -> dbFilter.apply(xref, databases))
                .filter(xref -> taxonFilter.apply(xref, taxonomyIds))
                .filter(xref -> statusFilter.apply(xref, request.getActive()))
                .filter(xref -> proteomeIdFilter.test(xref, proteomeId));
    }

    private List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list =
                    Arrays.stream(csv.split(COMMA_STR))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .toList();
        }
        return list;
    }

    private String searchUniParcId(String idField, String value) {
        try {
            String query = idField + ":" + value;
            SolrRequest solrRequest =
                    SolrRequest.builder().query(query).rows(NumberUtils.INTEGER_ONE).build();
            UniParcDocument document =
                    repository
                            .getEntry(solrRequest)
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return document.getUpi();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + value + "]";
            throw new ServiceException(message, e);
        }
    }
}
