package uk.ac.ebi.uniprot.uniprotkb.service;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.common.SequenceImpl;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.dataservice.serializer.avro.DefaultEntryConverter;
import uk.ac.ebi.uniprot.dataservice.serializer.impl.AvroByteArraySerializer;
import uk.ac.ebi.uniprot.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.services.data.serializer.model.entry.DefaultEntryObject;
import uk.ac.ebi.uniprot.uniprotkb.repository.store.UniProtStoreClient;
import uk.ac.ebi.uniprot.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.uniprotkb.service.filters.FilterComponentType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to simplify conversion of {@code QueryResult<UniProtDocument>} instances to
 * {@code QueryResult<UniProtEntry>} or {@code Optional<UniProtEntry>}. It is used in {@link UniProtEntryService}.
 *
 * Note, this class will be replaced once we finalise a core domain model, which is the single entity returned
 * from by requests -- and on which all message converters will operate.
 *
 * Created 18/10/18
 *
 * @author Edd
 */
class UniProtEntryQueryResultsConverter {
    private final UniProtStoreClient entryStore;
    private final AvroByteArraySerializer<DefaultEntryObject> serializer =
            AvroByteArraySerializer.instanceOf(DefaultEntryObject.class);
    private final DefaultEntryConverter entryConverter = new DefaultEntryConverter();
    private final RetryPolicy retryPolicy = new RetryPolicy()
            .retryOn(IOException.class)
            .withDelay(500,TimeUnit.MILLISECONDS)
            .withMaxRetries(5);

    UniProtEntryQueryResultsConverter(UniProtStoreClient entryStore) {
        this.entryStore = entryStore;
    }

    QueryResult<UniProtEntry> convertQueryResult(QueryResult<UniProtDocument> results, Map<String, List<String>> filters) {
        List<UniProtEntry> upEntries = results.getContent()
                .stream().map(doc -> convertDoc(doc, filters))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return QueryResult.of(upEntries, results.getPage(), results.getFacets());
    }

    Optional<UniProtEntry> convertDoc(UniProtDocument doc, Map<String, List<String>> filters) {
        if (doc.active) {
            return convert2UPEntry(doc, filters);
        } else {
            return Optional.empty();
        }
    }

    private Optional<UniProtEntry> convert2UPEntry(UniProtDocument doc, Map<String, List<String>> filters) {
        if (FieldsParser.isDefaultFilters(filters) && (doc.avro_binary != null)) {
            byte[] avroBinaryBytes = Base64.getDecoder().decode(doc.avro_binary.getBytes());
            DefaultEntryObject avroObject = serializer.fromByteArray(avroBinaryBytes);
            UniProtEntry uniEntry = entryConverter.fromAvro(avroObject);

            if (Objects.isNull(uniEntry)) {
                return Optional.empty();
            }
            if (filters.containsKey(FilterComponentType.MASS.name().toLowerCase())
                    || filters.containsKey(FilterComponentType.LENGTH.name().toLowerCase())) {
                char[] fakeSeqArrayWithCorrectLength = new char[doc.seqLength];
                Arrays.fill(fakeSeqArrayWithCorrectLength, 'X');
                SequenceImpl seq = new SequenceImpl();
                seq.setValue(new String(fakeSeqArrayWithCorrectLength));
                seq.setMolecularWeight(doc.seqMass);
                uniEntry.setSequence(seq);
            }
            return Optional.of(uniEntry);
        } else {
            //return entryStore.getEntry(doc.accession);            
            return Failsafe.with(retryPolicy).get(() -> entryStore.getEntry(doc.accession));                     
        }
    }
}
