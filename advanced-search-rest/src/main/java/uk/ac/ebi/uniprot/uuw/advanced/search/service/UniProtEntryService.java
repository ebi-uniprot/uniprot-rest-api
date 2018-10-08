package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Sequence;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.DefaultEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.impl.AvroByteArraySerializer;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.DefaultEntryObject;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.ListMessageConverter;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FilterComponentType;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.CloudSolrStreamTemplate;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.StoreStreamer;
import uk.ac.ebi.uniprot.uuw.advanced.search.store.UniProtStoreClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession";
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;
    private UniProtStoreClient entryService;
    private JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor;
    private final AvroByteArraySerializer<DefaultEntryObject> deSerialize = AvroByteArraySerializer
            .instanceOf(DefaultEntryObject.class);
    private final DefaultEntryConverter avroConverter = new DefaultEntryConverter();
    private final CloudSolrStreamTemplate cloudSolrStreamTemplate;
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final ThreadPoolTaskExecutor downloadTaskExecutor;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtStoreClient entryService,
                               JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor,
                               CloudSolrStreamTemplate cloudSolrStreamTemplate,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               ThreadPoolTaskExecutor downloadTaskExecutor) {
        this.repository = repository;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.entryService = entryService;
        this.uniProtJsonAdaptor = uniProtJsonAdaptor;
        this.cloudSolrStreamTemplate = cloudSolrStreamTemplate;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.downloadTaskExecutor = downloadTaskExecutor;
    }

    private void addSort(SimpleQuery simpleQuery, String sort) {
        List<Sort> sorts = UniProtSortUtil.createSort(sort);
        if (sorts.isEmpty()) {
            sorts = UniProtSortUtil.createDefaultSort();
        }
        sorts.forEach(simpleQuery::addSort);
    }

    public QueryResult<UPEntry> executeQuery(SearchRequestDTO cursorRequest) {
        String fields = cursorRequest.getField();
        Map<String, List<String>> filters = FieldsParser.parse(fields);
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(), uniprotFacetConfig).build();

            addSort(simpleQuery, cursorRequest.getSort());

            QueryResult<UniProtDocument> results = repository.searchPage(simpleQuery, cursorRequest.getCursor(),
                                                                         cursorRequest.getSize());
            return convertQueryDoc2UPEntry(results, filters);
        } catch (Exception e) {
            String message = "Could not get result for: [" + cursorRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    private QueryResult<UPEntry> convertQueryDoc2UPEntry(QueryResult<UniProtDocument> results,
                                                         Map<String, List<String>> filters) {
        List<UPEntry> upEntries = results.getContent().stream().map(doc -> convertDocToUPEntry(doc, filters))
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        return QueryResult.of(upEntries, results.getPage(), results.getFacets());
    }

    private Optional<UPEntry> convertDocToUPEntry(UniProtDocument doc, Map<String, List<String>> filters) {
        if (doc.active) {
            return convert2UPEntry(doc, filters);
        } else {
            return Optional.ofNullable(new UPEntry(doc.accession, doc.id, false, doc.inactiveReason));
        }
    }

    private Optional<UPEntry> convert2UPEntry(UniProtDocument doc, Map<String, List<String>> filters) {
        UPEntry entry = null;
        if (FieldsParser.isDefaultFilters(filters) && (doc.avro_binary != null)) {
            DefaultEntryObject avroObject = deSerialize.fromByteArray(doc.avro_binary);
            UniProtEntry uniEntry = avroConverter.fromAvro(avroObject);
            if (uniEntry == null)
                return Optional.empty();
            entry = convertAndFilter(uniEntry, filters);
            if (filters.containsKey(FilterComponentType.MASS.name().toLowerCase())
                    || filters.containsKey(FilterComponentType.LENGTH.name().toLowerCase())) {
                entry.setSequence(new Sequence(1, doc.seqLength, doc.seqMass, "", ""));
            }
        } else {
            Optional<UniProtEntry> opEntry = entryService.getEntry(doc.accession);
            if (opEntry.isPresent()) {
                entry = convertAndFilter(opEntry.get(), filters);
            }
        }
        if (entry != null) {
            entry.setAnnotationScore(doc.score);
        }
        return Optional.ofNullable(entry);
    }

    private UPEntry convertAndFilter(UniProtEntry upEntry, Map<String, List<String>> filterParams) {
        UPEntry entry = uniProtJsonAdaptor.convertEntity(upEntry, filterParams);
        if ((filterParams == null) || filterParams.isEmpty())
            return entry;

        EntryFilters.filterEntry(entry, filterParams);

        return entry;
    }

    public UPEntry getByAccession(String accession, String field) {
        UPEntry result = null;
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
            simpleQuery.addProjectionOnField(new SimpleField(ACCESSION));
            Optional<UniProtDocument> documentEntry = repository.getEntry(simpleQuery);
            if (documentEntry.isPresent()) {
                Map<String, List<String>> filters = FieldsParser.parse(field);
                result = convertDocToUPEntry(documentEntry.get(), filters).orElse(null);
            }
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
        return result;
    }

    public void streamForAccessions(List<String> accessions, MessageConverterContext context, ResponseBodyEmitter emitter) {
        String queryStr = "(" + accessions.stream()
                .map(acc -> "(" + ACCESSION + ":" + acc.toUpperCase() + ")")
                .collect(Collectors.joining(" OR ")) + ")";

        stream(queryStr, context, emitter);
    }

    public void stream(String query, MessageConverterContext context, ResponseBodyEmitter emitter) {
        MediaType contentType = context.getContentType();
        context.setEntities(streamEntities(query, contentType));

        downloadTaskExecutor.execute(() -> {
            try {
                emitter.send(context, contentType);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
        });
    }

    private Stream<?> streamEntities(String query, MediaType contentType) {
        CloudSolrStream cStream = cloudSolrStreamTemplate.create(query);
        try {
            cStream.open();
            if (contentType.equals(ListMessageConverter.LIST_MEDIA_TYPE)) {
                return storeStreamer.idsStream(cStream);
            } else {
                return storeStreamer.idsToStoreStream(cStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
