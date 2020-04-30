package org.uniprot.api.rest.output;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Ensure file extensions associated with media types are as expected, so that downloads produce
 * expected results.
 *
 * <p>Created 22/10/18
 *
 * @author Edd
 */
class UniProtMediaTypeTest {
    @Test
    void checkFlatfileFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(FF_MEDIA_TYPE), is("txt"));
    }

    @Test
    void checkExcelFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(XLS_MEDIA_TYPE), is("xlsx"));
    }

    @Test
    void checkListFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(LIST_MEDIA_TYPE), is("list"));
    }

    @Test
    void checkTsvFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(TSV_MEDIA_TYPE), is("tsv"));
    }

    @Test
    void checkFastaFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(FASTA_MEDIA_TYPE), is("fasta"));
    }

    @Test
    void checkGffFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(GFF_MEDIA_TYPE), is("gff"));
    }

    @Test
    void checkOboFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(OBO_MEDIA_TYPE), is("obo"));
    }

    @Test
    void checkRdfFileExtension() {
        assertThat(UniProtMediaType.getFileExtension(RDF_MEDIA_TYPE), is("rdf"));
    }

    @Test
    void checkUnknownMediaTypeFileExtension() {
        assertThat(
                UniProtMediaType.getFileExtension(new MediaType("type", "subtype")), is("subtype"));
    }

    @Test
    void checkFlatfileTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("txt"), is(FF_MEDIA_TYPE));
    }

    @Test
    void checkExcelTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("xlsx"), is(XLS_MEDIA_TYPE));
    }

    @Test
    void checkListTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("list"), is(LIST_MEDIA_TYPE));
    }

    @Test
    void checkTsvTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("tsv"), is(TSV_MEDIA_TYPE));
    }

    @Test
    void checkFastaTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("fasta"), is(FASTA_MEDIA_TYPE));
    }

    @Test
    void checkGffTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("gff"), is(GFF_MEDIA_TYPE));
    }

    @Test
    void checkOboTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("obo"), is(OBO_MEDIA_TYPE));
    }

    @Test
    void checkRdfTypeForFileExtension() {
        assertThat(UniProtMediaType.getMediaTypeForFileExtension("rdf"), is(RDF_MEDIA_TYPE));
    }

    @Test
    void checkUnknownFileExtension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UniProtMediaType.getMediaTypeForFileExtension("UNKNOWN"));
    }
}
