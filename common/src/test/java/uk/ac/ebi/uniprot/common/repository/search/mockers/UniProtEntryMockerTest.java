package uk.ac.ebi.uniprot.common.repository.search.mockers;

import org.junit.Test;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.ac.ebi.uniprot.common.repository.search.mockers.UniProtEntryMocker.Type.SP;

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
        assertThat(uniProtEntry.getPrimaryAccession().getValue(), is(accession));
    }

    @Test
    public void canCreateEntries() {
        Collection<UniProtEntry> entries = UniProtEntryMocker.createEntries();
        assertThat(entries, hasSize(greaterThan(0)));
    }
}