package org.uniprot.api.uniprotkb.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Query;
import org.springframework.stereotype.Service;
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
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.UniProtReference;
import org.uniprot.core.uniprot.builder.UniProtReferenceBuilder;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Service
public class PublicationService {

    private final UniProtKBStoreClient uniProtKBStore;
    private final LiteratureRepository repository;
    private final LiteratureStoreEntryConverter entryStoreConverter;
    private final SearchFieldConfig searchFieldConfig;

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

    public QueryResult<PublicationEntry> getPublicationsByUniprotAccession(
            final String accession, PublicationRequest request) {
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
        CursorPage page = getCursorPage(request, publications.size());
        publications = publications.subList(page.getOffset().intValue(), getPageTo(page));

        return QueryResult.of(publications, page, facets);
    }

    private List<PublicationEntry> getUniprotEntryPublicationEntries(String accession) {
        List<PublicationEntry> result = new ArrayList<>();
        Optional<UniProtEntry> uniProtEntry = uniProtKBStore.getEntry(accession);
        if (uniProtEntry.isPresent()) {
            UniProtEntry entry = uniProtEntry.get();
            if (entry.hasReferences()) {
                Map<Long, LiteratureEntry> literatureEntryMap =
                        getLiteraturesFromReferences(entry.getReferences());
                for (UniProtReference uniProtReference : entry.getReferences()) {
                    Long pubmedId = getPubmedId(uniProtReference);
                    result.add(
                            getPublicationEntry(
                                    literatureEntryMap.get(pubmedId),
                                    uniProtReference,
                                    entry.getEntryType().toDisplayName()));
                }
            }
        }
        return result;
    }

    private Map<Long, LiteratureEntry> getLiteraturesFromReferences(
            List<UniProtReference> references) {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        references.stream()
                .filter(this::hasPubmedId)
                .map(this::getPubmedId)
                .forEach(
                        pubmedId -> {
                            queryBuilder.add(
                                    new TermQuery(new Term("id", pubmedId.toString())),
                                    BooleanClause.Occur.SHOULD);
                        });

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

    private boolean hasPubmedId(UniProtReference uniProtReference) {
        return uniProtReference
                .getCitation()
                .getCitationCrossReferenceByType(CitationDatabase.PUBMED)
                .isPresent();
    }

    private Long getPubmedId(UniProtReference uniProtReference) {
        final Long[] result = {0L};
        uniProtReference
                .getCitation()
                .getCitationCrossReferenceByType(CitationDatabase.PUBMED)
                .ifPresent(xref -> result[0] = Long.valueOf(xref.getId()));
        return result[0];
    }

    private PublicationEntry getPublicationEntry(
            LiteratureEntry literatureEntry,
            UniProtReference uniProtReference,
            String publicationSource) {
        PublicationEntry.PublicationEntryBuilder builder = PublicationEntry.builder();
        UniProtReferenceBuilder referenceBuilder = UniProtReferenceBuilder.from(uniProtReference);
        if (Utils.notNull(literatureEntry)) {
            if (literatureEntry.hasCitation()) {
                referenceBuilder.citation(literatureEntry.getCitation());
            }
            if (literatureEntry.hasStatistics()) {
                builder.statistics(literatureEntry.getStatistics());
            }
        }
        return builder.reference(referenceBuilder.build())
                .categories(getCategoriesFromUniprotReference(uniProtReference))
                .publicationSource(publicationSource)
                .build();
    }

    private List<String> getCategoriesFromUniprotReference(UniProtReference uniProtReference) {
        Set<String> result = new HashSet<>();
        if (uniProtReference.hasReferencePositions()) {
            for (String position : uniProtReference.getReferencePositions()) {
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
                                    category -> {
                                        return Arrays.stream(PublicationCategory.values())
                                                .filter(a -> a.name().equalsIgnoreCase(category))
                                                .map(PublicationCategory::getLabel)
                                                .findFirst()
                                                .orElse("");
                                    })
                            .filter(Utils::notNullNotEmpty)
                            .collect(Collectors.toList());
            mappedReference.getSourceCategories().clear();
        }
        UniProtReference reference =
                new UniProtReferenceBuilder()
                        .citation(literatureEntry.getLiteratureEntry().getCitation())
                        .build();

        return PublicationEntry.builder()
                .reference(reference)
                .statistics(literatureEntry.getLiteratureEntry().getStatistics())
                .literatureMappedReference(mappedReference)
                .publicationSource("Computationally mapped")
                .categories(categories)
                .build();
    }

    private SolrRequest getSolrRequest(String query) {
        return SolrRequest.builder()
                .query(query)
                .addSort(
                        new Sort(
                                Sort.Direction.ASC,
                                this.searchFieldConfig
                                        .getSearchFieldItemByName("id")
                                        .getFieldName()))
                .defaultQueryOperator(Query.Operator.OR)
                .rows(100)
                .build();
    }

    private CursorPage getCursorPage(PublicationRequest request, int publicationSize) {
        CursorPage page = CursorPage.of(request.getCursor(), request.getSize());
        page.setTotalElements((long) publicationSize);
        page.setNextCursor("NEXT");
        return page;
    }

    private int getPageTo(CursorPage page) {
        long nextPageOffset = (long) page.getOffset() + page.getPageSize();
        if (nextPageOffset > page.getTotalElements()) {
            return page.getTotalElements().intValue();
        } else {
            return (int) nextPageOffset;
        }
    }

    private boolean isFromAccession(String accession, LiteratureMappedReference mappedReference) {
        return mappedReference.getUniprotAccession().getValue().equalsIgnoreCase(accession);
    }
}
