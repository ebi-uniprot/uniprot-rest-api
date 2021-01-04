package org.uniprot.api.uniparc.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.uniprot.core.Location;
import org.uniprot.core.Property;
import org.uniprot.core.Sequence;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.*;
import org.uniprot.core.uniparc.impl.*;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
public class UniParcControllerITUtils {

    public static UniParcEntry createEntry(int i, String upiRef) {
        StringBuilder seq = new StringBuilder("MLMPKRTKYR");
        IntStream.range(0, i).forEach(j -> seq.append("A"));
        Sequence sequence = new SequenceBuilder(seq.toString()).build();
        List<UniParcCrossReference> xrefs = getXrefs(i);

        List<SequenceFeature> seqFeatures = new ArrayList<>();
        Arrays.stream(SignatureDbType.values())
                .forEach(
                        signatureType -> {
                            seqFeatures.add(getSeqFeature(i, signatureType));
                        });
        List<Taxonomy> taxonomies = getTaxonomies();
        return new UniParcEntryBuilder()
                .uniParcId(new UniParcIdBuilder(getName(upiRef, i)).build())
                .uniParcCrossReferencesSet(xrefs)
                .sequence(sequence)
                .sequenceFeaturesSet(seqFeatures)
                .taxonomiesSet(taxonomies)
                .build();
    }

    static List<Taxonomy> getTaxonomies() {
        Taxonomy taxonomy =
                new TaxonomyBuilder().taxonId(9606).scientificName("Homo sapiens").build();
        Taxonomy taxonomy2 =
                new TaxonomyBuilder().taxonId(7787).scientificName("Torpedo californica").build();
        return Arrays.asList(taxonomy, taxonomy2);
    }

    static SequenceFeature getSeqFeature(int i, SignatureDbType signatureDbType) {
        List<Location> locations = Arrays.asList(new Location(12, 23), new Location(45, 89));
        InterProGroup domain =
                new InterProGroupBuilder()
                        .name(getName("Inter Pro Name", i))
                        .id(getName("IP0000", i))
                        .build();
        return new SequenceFeatureBuilder()
                .interproGroup(domain)
                .signatureDbType(signatureDbType)
                .signatureDbId(getName("SIG0000", i))
                .locationsSet(locations)
                .build();
    }

    static List<UniParcCrossReference> getXrefs(int i) {
        List<Property> properties = new ArrayList<>();
        properties.add(
                new Property(
                        UniParcCrossReference.PROPERTY_PROTEIN_NAME, getName("proteinName", i)));
        properties.add(
                new Property(UniParcCrossReference.PROPERTY_GENE_NAME, getName("geneName", i)));
        properties.add(
                new Property(UniParcCrossReference.PROPERTY_PROTEOME_ID, getName("UP1234567", i)));
        properties.add(new Property(UniParcCrossReference.PROPERTY_CHAIN, "chain"));
        UniParcCrossReference xref =
                new UniParcCrossReferenceBuilder()
                        .versionI(3)
                        .database(UniParcDatabase.SWISSPROT)
                        .id(getName("P100", i))
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 5, 17))
                        .lastUpdated(LocalDate.of(2017, 2, 27))
                        .propertiesSet(properties)
                        .build();

        List<Property> properties2 = new ArrayList<>();
        properties2.add(
                new Property(
                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                        getName("anotherProteinName", i)));
        properties2.add(new Property(UniParcCrossReference.PROPERTY_NCBI_TAXONOMY_ID, "9606"));
        properties2.add(new Property(UniParcCrossReference.PROPERTY_NCBI_TAXONOMY_ID, "7787"));
        properties2.add(new Property(UniParcCrossReference.PROPERTY_PROTEOME_ID, "UP000005640"));

        UniParcCrossReference xref2 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.TREMBL)
                        .id(getName("P123", i))
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesSet(properties2)
                        .build();

        return Arrays.asList(xref, xref2);
    }

    static UniParcEntry createEntry(
            String id, int sequenceLength, List<UniParcCrossReference> crossReferences) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        StringBuilder sequence = new StringBuilder();
        IntStream.range(0, sequenceLength).forEach(i -> sequence.append("A"));
        Sequence uniSeq = new SequenceBuilder(sequence.toString()).build();
        builder.uniParcId(id).sequence(uniSeq).uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }

    static UniParcCrossReference getXref(
            UniParcDatabase database, String id, Integer taxId, boolean active) {
        UniParcCrossReferenceBuilder xrefBuilder = new UniParcCrossReferenceBuilder();
        xrefBuilder
                .database(database)
                .id(id)
                .versionI(1)
                .version(7)
                .active(active)
                .created(LocalDate.of(2017, 2, 12))
                .lastUpdated(LocalDate.of(2017, 4, 23));
        List<Property> properties = new ArrayList<>();
        properties.add(
                new Property(UniParcCrossReference.PROPERTY_NCBI_TAXONOMY_ID, taxId.toString()));
        properties.add(new Property(UniParcCrossReference.PROPERTY_PROTEIN_NAME, "protein Name"));
        properties.add(new Property(UniParcCrossReference.PROPERTY_GENE_NAME, "Gel"));
        properties.add(new Property(UniParcCrossReference.PROPERTY_PROTEOME_ID, "UPI"));
        xrefBuilder.propertiesSet(properties);

        return xrefBuilder.build();
    }

    static String getName(String prefix, int i) {
        if (i < 10) {
            return prefix + "0" + i;
        } else return prefix + i;
    }

    static UniParcEntry appendMoreXRefs(UniParcEntry entry, int i) {
        UniParcCrossReference xref1 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.EMBL)
                        .id("embl" + i)
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();

        UniParcCrossReference xref2 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.UNIMES)
                        .id("unimes" + i)
                        .version(7)
                        .active(false)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "proteinName" + i))
                        .build();

        // common db xref
        UniParcCrossReference xref3 =
                new UniParcCrossReferenceBuilder()
                        .versionI(1)
                        .database(UniParcDatabase.VECTORBASE)
                        .id("common-vector")
                        .version(7)
                        .active(true)
                        .created(LocalDate.of(2017, 2, 12))
                        .lastUpdated(LocalDate.of(2017, 4, 23))
                        .propertiesAdd(
                                new Property(
                                        UniParcCrossReference.PROPERTY_PROTEIN_NAME,
                                        "common-vector-proteinName" + i))
                        .build();
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(entry);
        builder.uniParcCrossReferencesAdd(xref1);
        builder.uniParcCrossReferencesAdd(xref2);
        builder.uniParcCrossReferencesAdd(xref3);
        return builder.build();
    }
}
