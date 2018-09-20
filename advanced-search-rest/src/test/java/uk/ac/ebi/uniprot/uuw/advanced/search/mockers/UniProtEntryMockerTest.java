package uk.ac.ebi.uniprot.uuw.advanced.search.mockers;

import org.junit.Test;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;

import java.util.Collection;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker.Type.SP;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created 19/09/18
 *
 * @author Edd
 */
public class UniProtEntryMockerTest {
    @Test
    public void canCreateSP() {
        UniProtEntry uniProtEntry = UniProtEntryMocker.create(SP);
        assertThat(uniProtEntry, is(notNullValue()));
    }

    @Test
    public void canCreateEntryWithAccession() {
        String accession = "P12345";
        UniProtEntry uniProtEntry = UniProtEntryMocker.create(accession);
        assertThat(uniProtEntry, is(notNullValue()));
        assertThat(uniProtEntry.getPrimaryUniProtAccession().getValue(), is(accession));
    }

    @Test
    public void canCreateEntries() {
        Collection<UniProtEntry> entries = UniProtEntryMocker.createEntries();
        assertThat(entries, hasSize(greaterThan(0)));
    }
}