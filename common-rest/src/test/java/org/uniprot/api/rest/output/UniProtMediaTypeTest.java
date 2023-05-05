package org.uniprot.api.rest.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

/**
 * Ensure file extensions associated with media types are as expected, so that downloads produce
 * expected results.
 *
 * <p>Created 22/10/18
 *
 * @author Edd
 */
class UniProtMediaTypeTest {
    @ParameterizedTest
    @CsvSource({
        FF_MEDIA_TYPE_VALUE + ",txt",
        LIST_MEDIA_TYPE_VALUE + ",list",
        TSV_MEDIA_TYPE_VALUE + ",tsv",
        XLS_MEDIA_TYPE_VALUE + ",xlsx",
        FASTA_MEDIA_TYPE_VALUE + ",fasta",
        GFF_MEDIA_TYPE_VALUE + ",gff",
        OBO_MEDIA_TYPE_VALUE + ",obo",
        RDF_MEDIA_TYPE_VALUE + ",rdf",
        TURTLE_MEDIA_TYPE_VALUE + ",ttl",
        N_TRIPLES_MEDIA_TYPE_VALUE + ",nt",
        MARKDOWN_MEDIA_TYPE_VALUE + ",md",
        APPLICATION_XML_VALUE + ",xml",
        APPLICATION_JSON_VALUE + ",json",
        HDF5_MEDIA_TYPE_VALUE + ",h5"
    })
    void checkMediaTypeFileExtensionRoundTrip(String mediaTypeStr, String extension) {
        MediaType mediaType = valueOf(mediaTypeStr);
        assertThat(UniProtMediaType.getFileExtension(mediaType), is(extension));
        assertThat(UniProtMediaType.getMediaTypeForFileExtension(extension), is(mediaType));
    }

    @Test
    void checkUnknownMediaTypeFileExtension() {
        assertThat(
                UniProtMediaType.getFileExtension(new MediaType("type", "subtype")), is("subtype"));
    }

    @Test
    void checkUnknownFileExtension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UniProtMediaType.getMediaTypeForFileExtension("UNKNOWN"));
    }
}
