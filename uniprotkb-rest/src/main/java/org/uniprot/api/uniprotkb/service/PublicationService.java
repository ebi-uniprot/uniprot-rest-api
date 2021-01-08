package org.uniprot.api.uniprotkb.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryOperator;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationCategory;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBReference;
import org.uniprot.core.uniprotkb.impl.UniProtKBReferenceBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Service
public class PublicationService {

    private static final String COMPUTATIONALLY_MAPPED = "Computationally mapped";
    private final UniProtKBStoreClient uniProtKBStore;
    private final LiteratureRepository repository;
    private final LiteratureStoreEntryConverter entryStoreConverter;
    private final SearchFieldConfig searchFieldConfig;

    @Value("${search.default.page.size:#{null}}")
    protected Integer defaultPageSize;

    public PublicationService(
            UniProtKBStoreClient entryStore,
            LiteratureRepository repository,
            LiteratureStoreEntryConverter entryStoreConverter) {
        this.uniProtKBStore = entryStore;
        this.repository = repository;
        this.entryStoreConverter = entryStoreConverter;
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE);
    }

    // TODO: 06/01/2021 take a look at

    public QueryResult<PublicationEntry> getPublicationsByUniprotAccession(
            final String accession, PublicationRequest request) {
        // TODO: 04/01/2021
        // docs <- search solr q=accession:$accession&sort=type desc,reference_number asc,pubmed_id
        // desc&facet.field=categories&facet.field=types
        // for each doc i $docs
        //   construct publication entry object from $doc
        //   hmmm, how do we get publication title -- call literature service -- but could change this in future
        //   and store it in the publicationdocument binary

        // LOAD THE DATA
        List<PublicationEntry> publications = new ArrayList<>();
        publications.addAll(getUniprotEntryPublicationEntries(accession));
        publications.addAll(getMappedPublicationEntries(accession));

        // APPLY FACET FILTERS
        PublicationFacetConfig.applyFacetFilters(publications, request);

        // CREATE FACETS
        List<Facet> facets = new ArrayList<>();
        if (Utils.notNullNotEmpty(request.getFacets())) {
            facets.addAll(PublicationFacetConfig.getFacets(publications, request.getFacets()));
        }

        // PAGINATE THE RESULT
        CursorPage page = getPage(request, publications);
        publications =
                publications.subList(page.getOffset().intValue(), CursorPage.getNextOffset(page));

        return QueryResult.of(publications.stream(), page, facets);
    }

    private CursorPage getPage(PublicationRequest request, List<PublicationEntry> publications) {
        if (request.getSize() == null) {
            request.setSize(defaultPageSize);
        }
        return CursorPage.of(request.getCursor(), request.getSize(), publications.size());
    }

    private List<PublicationEntry> getUniprotEntryPublicationEntries(String accession) {
        List<PublicationEntry> result = new ArrayList<>();
        Optional<UniProtKBEntry> uniProtEntry = uniProtKBStore.getEntry(accession);
        if (uniProtEntry.isPresent()) {
            UniProtKBEntry entry = uniProtEntry.get();
            if (entry.hasReferences()) {
                Map<Long, LiteratureEntry> literatureEntryMap =
                        getLiteraturesFromReferences(entry.getReferences());
                for (UniProtKBReference uniProtkbReference : entry.getReferences()) {
                    Long pubmedId = getPubmedId(uniProtkbReference);
                    result.add(
                            getPublicationEntry(
                                    literatureEntryMap.get(pubmedId),
                                    uniProtkbReference,
                                    entry.getEntryType().getDisplayName()));
                }
            }
        }
        return result;
    }

    private Map<Long, LiteratureEntry> getLiteraturesFromReferences(
            List<UniProtKBReference> references) {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        references.stream()
                .filter(this::hasPubmedId)
                .map(this::getPubmedId)
                .forEach(
                        pubmedId ->
                                queryBuilder.add(
                                        new TermQuery(new Term("id", pubmedId.toString())),
                                        BooleanClause.Occur.SHOULD));

        SolrRequest solrRequest = getSolrRequest(queryBuilder.build().toString());
        Stream<LiteratureDocument> literatures = repository.getAll(solrRequest);
        return literatures
                .map(entryStoreConverter)
                .map(LiteratureStoreEntry::getLiteratureEntry)
                .collect(Collectors.toMap(this::getPubmedIdFromEntry, Function.identity()));
    }

    private Long getPubmedIdFromEntry(LiteratureEntry entry) {
        Literature literature = (Literature) entry.getCitation();
        return literature.getPubmedId();
    }

    private boolean hasPubmedId(UniProtKBReference uniProtkbReference) {
        return uniProtkbReference
                .getCitation()
                .getCitationCrossReferenceByType(CitationDatabase.PUBMED)
                .isPresent();
    }

    private Long getPubmedId(UniProtKBReference uniProtkbReference) {
        final Long[] result = {0L};
        uniProtkbReference
                .getCitation()
                .getCitationCrossReferenceByType(CitationDatabase.PUBMED)
                .ifPresent(xref -> result[0] = Long.valueOf(xref.getId()));
        return result[0];
    }

    private PublicationEntry getPublicationEntry(
            LiteratureEntry literatureEntry,
            UniProtKBReference uniProtkbReference,
            String publicationSource) {
        PublicationEntry.PublicationEntryBuilder builder = PublicationEntry.builder();
        UniProtKBReferenceBuilder referenceBuilder =
                UniProtKBReferenceBuilder.from(uniProtkbReference);
        if (Utils.notNull(literatureEntry)) {
            if (literatureEntry.hasCitation()) {
                referenceBuilder.citation(literatureEntry.getCitation());
            }
            if (literatureEntry.hasStatistics()) {
                builder.statistics(literatureEntry.getStatistics());
            }
        }
        return builder.reference(referenceBuilder.build())
                .categories(getCategoriesFromUniprotReference(uniProtkbReference))
                .publicationSource(publicationSource)
                .build();
    }

    private List<String> getCategoriesFromUniprotReference(UniProtKBReference uniProtkbReference) {
        Set<String> result = new HashSet<>();
        if (uniProtkbReference.hasReferencePositions()) {
            for (String position : uniProtkbReference.getReferencePositions()) {
                for (PublicationCategory category : PublicationCategory.values()) {
                    for (String categoryText : category.getFunctionTexts()) {
                        if (position.toUpperCase().contains(categoryText)) {
                            result.add(category.getLabel());
                        }
                    }
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<PublicationEntry> getMappedPublicationEntries(String accession) {
        SolrRequest solrRequest = getSolrRequest("mapped_protein:" + accession);
        return repository
                .getAll(solrRequest)
                .map(
                        literatureDocument ->
                                mapLiteratureToPublication(accession, literatureDocument))
                .collect(Collectors.toList());
    }

    private PublicationEntry mapLiteratureToPublication(
            String accession, LiteratureDocument literatureDocument) {
        LiteratureStoreEntry literatureEntry = entryStoreConverter.apply(literatureDocument);
        literatureEntry
                .getLiteratureMappedReferences()
                .removeIf(mappedReference -> !isFromAccession(accession, mappedReference));

        LiteratureMappedReference mappedReference =
                literatureEntry.getLiteratureMappedReferences().get(0);
        List<String> categories = new ArrayList<>();
        if (mappedReference.hasSourceCategory()) {
            categories =
                    mappedReference.getSourceCategories().stream()
                            .map(
                                    category ->
                                            Arrays.stream(PublicationCategory.values())
                                                    .filter(
                                                            a ->
                                                                    a.name()
                                                                            .equalsIgnoreCase(
                                                                                    category))
                                                    .map(PublicationCategory::getLabel)
                                                    .findFirst()
                                                    .orElse(""))
                            .filter(Utils::notNullNotEmpty)
                            .collect(Collectors.toList());
            mappedReference.getSourceCategories().clear();
        }
        UniProtKBReference reference =
                new UniProtKBReferenceBuilder()
                        .citation(literatureEntry.getLiteratureEntry().getCitation())
                        .build();

        return PublicationEntry.builder()
                .reference(reference)
                .statistics(literatureEntry.getLiteratureEntry().getStatistics())
                .literatureMappedReference(mappedReference)
                .publicationSource(COMPUTATIONALLY_MAPPED)
                .categories(categories)
                .build();
    }

    private SolrRequest getSolrRequest(String query) {
        return SolrRequest.builder()
                .query(query)
                .sort(
                        SolrQuery.SortClause.asc(
                                this.searchFieldConfig
                                        .getSearchFieldItemByName("id")
                                        .getFieldName()))
                .defaultQueryOperator(QueryOperator.OR)
                .rows(100)
                .build();
    }

    private boolean isFromAccession(String accession, LiteratureMappedReference mappedReference) {
        return mappedReference.getUniprotAccession().getValue().equalsIgnoreCase(accession);
    }
}
