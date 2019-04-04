package uk.ac.ebi.uniprot.api.common.repository.search.mockers;

import uk.ac.ebi.uniprot.dataservice.source.impl.inactiveentry.InactiveUniProtEntry;

import java.util.*;

/**
 * This class is responsible to create mock objects for inactive UniProt entries.
 *
 * @author lgonzales
 */
public class InactiveEntryMocker {

    public enum InactiveType {
        DELETED,MERGED,DEMERGED;
    }

    private static Map<InactiveType, List<InactiveUniProtEntry>> entryMap = new HashMap<>();
    //Initialize Mocked InactiveUniProtEntry
    static {
        List<InactiveUniProtEntry> deletedEntries = new ArrayList<>();
        deletedEntries.add(InactiveUniProtEntry.from("I8FBX0","I8FBX0_MYCAB","deleted",null));
        deletedEntries.add(InactiveUniProtEntry.from("I8FBX1","I8FBX1_YERPE","deleted",null));
        deletedEntries.add(InactiveUniProtEntry.from("I8FBX2","I8FBX2_YERPE","deleted",null));
        entryMap.put(InactiveType.DELETED,deletedEntries);


        List<InactiveUniProtEntry> mergedEntries = new ArrayList<>();
        mergedEntries.add(InactiveUniProtEntry.from("Q14301","Q14301_FGFR2","merged","P21802"));
        mergedEntries.add(InactiveUniProtEntry.from("B4DFC2","B4DFC2_FGFR2","merged","P21802"));
        mergedEntries.add(InactiveUniProtEntry.from("F8VPU5","F8VPU5_BRCA2","merged","P97929"));
        entryMap.put(InactiveType.MERGED,mergedEntries);

        List<InactiveUniProtEntry> demergedEntries = new ArrayList<>();
        demergedEntries.add(InactiveUniProtEntry.from("Q00007","2ABA_HUMAN","merged","P63150"));
        demergedEntries.add(InactiveUniProtEntry.from("Q00007","2ABA_HUMAN","merged","P63151"));
        entryMap.put(InactiveType.DEMERGED, Collections.singletonList(InactiveUniProtEntry.merge(demergedEntries)));
    }
    public static List<InactiveUniProtEntry> create(InactiveType inactiveType){
        return entryMap.get(inactiveType);
    }

}
