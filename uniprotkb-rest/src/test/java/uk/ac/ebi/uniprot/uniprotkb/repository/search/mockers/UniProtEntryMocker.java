package uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers;


import org.apache.commons.io.IOUtils;
import uk.ac.ebi.uniprot.domain.builder.SequenceBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntryType;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtAccessionBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtEntryBuilder;
import uk.ac.ebi.uniprot.flatfile.parser.UniProtParser;
import uk.ac.ebi.uniprot.flatfile.parser.impl.DefaultUniProtParser;
import uk.ac.ebi.uniprot.flatfile.parser.impl.SupportingDataMapImpl;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 19/09/18
 *
 * @author Edd
 */
public class UniProtEntryMocker {
    public enum Type {
        SP("Q8DIA7.dat"), SP_COMPLEX("P97929.dat"), TR("F1Q0X3.dat"),
        SP_CANONICAL("P21802.dat"),SP_ISOFORM("P21802-2.dat"),SP_CANONICAL_ISOFORM("P21802-1.dat"),
        WITH_DEMERGED_SEC_ACCESSION("P63150.dat");

        private final String fileName;

        Type(String fileName) {
            this.fileName = fileName;
        }
    }

    private static Map<Type, UniProtEntry> entryMap = new HashMap<>();

    static {
        for (Type type : Type.values()) {
            InputStream is = UniProtEntryMocker.class.getResourceAsStream("/entry/" + type.fileName);
            try {
                UniProtParser parser = new DefaultUniProtParser(new SupportingDataMapImpl(),true);
                UniProtEntry entry = parser.parse(IOUtils.toString(is, Charset.defaultCharset()));
                entryMap.put(type, entry);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static UniProtEntry create(String accession) {
        UniProtEntry entry =  entryMap.get(Type.SP);
        UniProtEntryBuilder builder = new UniProtEntryBuilder().from(entry);
        return builder.primaryAccession(new UniProtAccessionBuilder(accession).build())
                .uniProtId(entry.getUniProtId())
                .active()
                .entryType(UniProtEntryType.TREMBL)
                .sequence(new SequenceBuilder("AAAAA").build())
                .build();
    }

    public static UniProtEntry create(Type type) {
        UniProtEntryBuilder.ActiveEntryBuilder builder = new UniProtEntryBuilder().from(entryMap.get(type));
        return builder.build();
    }

    public static Collection<UniProtEntry> createEntries() {
        return entryMap.values();
    }
}
