package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Sequence;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.DefaultEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.impl.AvroByteArraySerializer;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.DefaultEntryObject;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.EntryFilters;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FilterComponentType;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class UniProtEntryService {
	private static final String ACCESSION = "accession";
	private UniprotQueryRepository repository;
	private UniprotFacetConfig uniprotFacetConfig;
	private VoldemortClient<UniProtEntry> entryService;
	private JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor;
	private final AvroByteArraySerializer<DefaultEntryObject> deSerialize = AvroByteArraySerializer
			.instanceOf(DefaultEntryObject.class);
	private final DefaultEntryConverter avroConverter = new DefaultEntryConverter();

	public UniProtEntryService(UniprotQueryRepository repository, UniprotFacetConfig uniprotFacetConfig,
			VoldemortClient<UniProtEntry> entryService, JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor) {
		this.repository = repository;
		this.uniprotFacetConfig = uniprotFacetConfig;
		this.entryService = entryService;
		this.uniProtJsonAdaptor = uniProtJsonAdaptor;
	}

	public QueryResult<UPEntry> executeQuery(QuerySearchRequest searchRequest) {
		String fields = searchRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		String sort = searchRequest.getSort();
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery(), uniprotFacetConfig).build();
			addSort(simpleQuery, sort);
			QueryResult<UniProtDocument> results = repository.searchPage(simpleQuery, searchRequest.getOffset(),
					searchRequest.getSize());
			return convertQueryDoc2UPEntry(results, filters);
		} catch (Exception e) {
			String message = "Could not get result for: [" + searchRequest + "]";
			throw new ServiceException(message, e);
		}
	}

	private void addSort(SimpleQuery simpleQuery, String sort) {
		List<Sort> sorts = UniProtSortUtil.createSort(sort);
		if(sorts.isEmpty()) {
			sorts = UniProtSortUtil.createDefaultSort();
		}
		sorts.forEach(simpleQuery::addSort);
	}

	public Stream<UPEntry> getAll(String query) {
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(query, uniprotFacetConfig).build();
			addSort(simpleQuery, "");
			Cursor<UniProtDocument> results = repository.getAll(simpleQuery);
			return convertStreamDoc2UPEntry(results, Collections.emptyMap());
		} catch (Exception e) {
			String message = "Could not get result for: [" + query + "]";
			throw new ServiceException(message, e);
		}
	}

	public QueryResult<UPEntry> executeCursorQuery(QueryCursorRequest cursorRequest) {
		String fields = cursorRequest.getField();
		Map<String, List<String>> filters = FieldsParser.parse(fields);
		try {
			SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(), uniprotFacetConfig).build();

			addSort(simpleQuery, cursorRequest.getSort());

			QueryResult<UniProtDocument> results = repository.searchCursorPage(simpleQuery, cursorRequest.getCursor(),
					cursorRequest.getSize());
			return convertQueryDoc2UPEntry(results, filters);
		} catch (Exception e) {
			String message = "Could not get result for: [" + cursorRequest + "]";
			throw new ServiceException(message, e);
		}
	}

	private Stream<UPEntry> convertStreamDoc2UPEntry(Cursor<UniProtDocument> cursor,
			Map<String, List<String>> filters) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false)
				.map(doc -> convertDocToUPEntry(doc, filters))
				.flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty());

	}

	private QueryResult<UPEntry> convertQueryDoc2UPEntry(QueryResult<UniProtDocument> results,
			Map<String, List<String>> filters) {
		List<UPEntry> upEntries = results.getContent().stream().map(doc -> convertDocToUPEntry(doc, filters))
				.filter(val -> val.isPresent()).map(val -> val.get()).collect(Collectors.toList());
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

	public UPEntry convertAndFilter(UniProtEntry upEntry, Map<String, List<String>> filterParams) {
		UPEntry entry = uniProtJsonAdaptor.convertEntity(upEntry, filterParams);
		if ((filterParams == null) || filterParams.isEmpty())
			return entry;

		EntryFilters.filterEntry(entry, filterParams);

		return entry;
	}

	public Optional<UniProtEntry> getByAccession(String accession) {
		try {
			SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
			simpleQuery.addProjectionOnField(new SimpleField(ACCESSION));
			Optional<UniProtDocument> result = repository.getEntry(simpleQuery);
			return result.isPresent() ? entryService.getEntry(result.get().accession) : Optional.empty();
		} catch (Exception e) {
			String message = "Could not get accession for: [" + accession + "]";
			throw new ServiceException(message, e);
		}
	}

}
