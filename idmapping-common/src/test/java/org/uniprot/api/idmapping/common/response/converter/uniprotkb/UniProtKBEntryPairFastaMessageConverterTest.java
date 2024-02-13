package org.uniprot.api.idmapping.common.response.converter.uniprotkb;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniprotkb.*;
import org.uniprot.core.uniprotkb.description.FlagType;
import org.uniprot.core.uniprotkb.description.impl.NameBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinDescriptionBuilder;
import org.uniprot.core.uniprotkb.description.impl.ProteinNameBuilder;
import org.uniprot.core.uniprotkb.impl.*;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;

class UniProtKBEntryPairFastaMessageConverterTest {

    @Test
    void toSubsequenceFasta() throws IOException {
        UniProtKBEntryPairFastaMessageConverter converter =
                new UniProtKBEntryPairFastaMessageConverter();

        MessageConverterContext<UniProtKBEntryPair> context =
                MessageConverterContext.<UniProtKBEntryPair>builder().subsequence(true).build();
        converter.before(context, null);

        UniProtKBEntry entry =
                new UniProtKBEntryBuilder("P21802", "P21802_HUMAN", UniProtKBEntryType.SWISSPROT)
                        .sequence(new SequenceBuilder("ABCDEFGHIJKLMNOPQRSTUVXZ").build())
                        .entryAudit(new EntryAuditBuilder().sequenceVersion(2).build())
                        .proteinDescription(
                                new ProteinDescriptionBuilder()
                                        .flag(FlagType.FRAGMENTS_PRECURSOR)
                                        .recommendedName(
                                                new ProteinNameBuilder()
                                                        .fullName(
                                                                new NameBuilder()
                                                                        .value("Rec Name Value")
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();
        UniProtKBEntryPair entryPair =
                UniProtKBEntryPair.builder().from("P21802[5-10]").to(entry).build();
        String result = converter.toFasta(entryPair);
        assertNotNull(result);
        assertEquals(">sp|P21802|5-10\nEFGHIJ", result);
    }

    @Test
    void toFasta() throws IOException {
        UniProtKBEntryPairFastaMessageConverter converter =
                new UniProtKBEntryPairFastaMessageConverter();

        MessageConverterContext<UniProtKBEntryPair> context =
                MessageConverterContext.<UniProtKBEntryPair>builder().subsequence(false).build();
        converter.before(context, null);

        UniProtKBEntry entry =
                new UniProtKBEntryBuilder("P21802", "P21802_HUMAN", UniProtKBEntryType.SWISSPROT)
                        .genesAdd(
                                new GeneBuilder()
                                        .geneName(new GeneNameBuilder().value("P53").build())
                                        .build())
                        .proteinDescription(
                                new ProteinDescriptionBuilder()
                                        .recommendedName(
                                                new ProteinNameBuilder()
                                                        .fullName(
                                                                new NameBuilder()
                                                                        .value("PName")
                                                                        .build())
                                                        .build())
                                        .build())
                        .organism(
                                new OrganismBuilder()
                                        .taxonId(9606L)
                                        .scientificName("OName")
                                        .build())
                        .proteinExistence(ProteinExistence.HOMOLOGY)
                        .sequence(new SequenceBuilder("ABCDEFGHIJKLMNOPQRSTUVXZ").build())
                        .entryAudit(new EntryAuditBuilder().sequenceVersion(2).build())
                        .build();
        UniProtKBEntryPair entryPair =
                UniProtKBEntryPair.builder().from("P21802").to(entry).build();
        String result = converter.toFasta(entryPair);
        assertNotNull(result);
        assertEquals(
                ">sp|P21802|P21802_HUMAN PName OS=OName OX=9606 GN=P53 PE=3 SV=2\nABCDEFGHIJKLMNOPQRSTUVXZ",
                result);
    }

    @Test
    void toFastaInactive() throws IOException {
        UniProtKBEntryPairFastaMessageConverter converter =
                new UniProtKBEntryPairFastaMessageConverter();

        MessageConverterContext<UniProtKBEntryPair> context =
                MessageConverterContext.<UniProtKBEntryPair>builder().subsequence(false).build();
        converter.before(context, null);

        EntryInactiveReason reason =
                new EntryInactiveReasonBuilder().type(InactiveReasonType.DELETED).build();
        UniProtKBEntry entry = new UniProtKBEntryBuilder("P21802", "P21802_HUMAN", reason).build();
        UniProtKBEntryPair entryPair =
                UniProtKBEntryPair.builder().from("P21802").to(entry).build();
        String result = converter.toFasta(entryPair);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
