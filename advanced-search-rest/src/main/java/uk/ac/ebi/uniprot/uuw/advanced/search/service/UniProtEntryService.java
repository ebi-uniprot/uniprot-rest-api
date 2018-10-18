package uk.ac.ebi.uniprot.uuw.advanced.search.service;

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
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FilterComponentType;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.StoreStreamer;
import uk.ac.ebi.uniprot.uuw.advanced.search.store.UniProtStoreClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.*;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession";
    private final AvroByteArraySerializer<DefaultEntryObject> deSerialize = AvroByteArraySerializer
            .instanceOf(DefaultEntryObject.class);
    private final DefaultEntryConverter avroConverter = new DefaultEntryConverter();
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final ThreadPoolTaskExecutor downloadTaskExecutor;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;
    private UniProtStoreClient entryService;
    private JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtStoreClient entryService,
                               JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               ThreadPoolTaskExecutor downloadTaskExecutor) {
        this.repository = repository;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.entryService = entryService;
        this.uniProtJsonAdaptor = uniProtJsonAdaptor;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.downloadTaskExecutor = downloadTaskExecutor;
    }

    public QueryResult<UPEntry> executeQuery(SearchRequestDTO request) {
        String fields = request.getFields();
        Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(request.getQuery(), uniprotFacetConfig).build();

            simpleQuery.addSort(getUniProtSort(request.getSort()));

            QueryResult<UniProtDocument> results = repository.searchPage(simpleQuery, request.getCursor(),
                                                                         request.getSize());
            return convertQueryDoc2UPEntry(results, filters);
        } catch (Exception e) {
            String message = "Could not get result for: [" + request + "]";
            throw new ServiceException(message, e);
        }
    }

    public UPEntry getByAccession(String accession, String fields) {
        UPEntry result = null;
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
            simpleQuery.addProjectionOnField(new SimpleField(ACCESSION));
            Optional<UniProtDocument> documentEntry = repository.getEntry(simpleQuery);
            if (documentEntry.isPresent()) {
                Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
                result = convertDocToUPEntry(documentEntry.get(), filters).orElse(null);
            }
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
        return result;
    }

    public void stream(SearchRequestDTO request, MessageConverterContext context, ResponseBodyEmitter emitter) {
        MediaType contentType = context.getContentType();
        boolean defaultFieldsRequested = FieldsParser.isDefaultFilters(FieldsParser.parseForFilters(request.getFields()));
        context.setEntities(streamEntities(request, defaultFieldsRequested, contentType));

        downloadTaskExecutor.execute(() -> {
            try {
                emitter.send(context, contentType);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
        });
    }

    private Sort getUniProtSort(String sortStr) {
        return UniProtSortUtil.createSort(sortStr).orElse(UniProtSortUtil.createDefaultSort());
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
            byte[] avroBinaryBytes = Base64.getDecoder().decode(doc.avro_binary.getBytes());
            DefaultEntryObject avroObject = deSerialize.fromByteArray(avroBinaryBytes);
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

    private Stream<?> streamEntities(SearchRequestDTO request, boolean defaultFieldsOnly, MediaType contentType) {
        String query = request.getQuery();
        Sort sort = getUniProtSort(request.getSort());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            return storeStreamer.idsStream(query, sort);
        }
        if (defaultFieldsOnly && (contentType.equals(MediaType.APPLICATION_JSON) || contentType.equals(TSV_MEDIA_TYPE) 
        		||contentType.equals(XLS_MEDIA_TYPE)  )) {
            return storeStreamer.defaultFieldStream(query, sort);
        } else {
            return storeStreamer.idsToStoreStream(query, sort);
        }
    }
}
