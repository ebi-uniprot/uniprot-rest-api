package org.uniprot.api.rest.output;

import com.google.common.collect.HashBiMap;
import org.springframework.http.MediaType;

import java.util.Collection;
import java.util.Objects;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_MARKDOWN_VALUE;

public class UniProtMediaType {
    public static final String DEFAULT_MEDIA_TYPE_VALUE = APPLICATION_JSON_VALUE;
    public static final MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON;
    public static final String FF_MEDIA_TYPE_VALUE = "text/plain;format=flatfile";
    public static final MediaType FF_MEDIA_TYPE = valueOf(FF_MEDIA_TYPE_VALUE);
    public static final String LIST_MEDIA_TYPE_VALUE = "text/plain;format=list";
    public static final MediaType LIST_MEDIA_TYPE = valueOf(LIST_MEDIA_TYPE_VALUE);
    public static final String TSV_MEDIA_TYPE_VALUE = "text/plain;format=tsv";
    public static final MediaType TSV_MEDIA_TYPE = valueOf(TSV_MEDIA_TYPE_VALUE);
    public static final String XLS_MEDIA_TYPE_VALUE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final MediaType XLS_MEDIA_TYPE = valueOf(XLS_MEDIA_TYPE_VALUE);
    public static final String FASTA_MEDIA_TYPE_VALUE = "text/plain;format=fasta";
    public static final MediaType FASTA_MEDIA_TYPE = valueOf(FASTA_MEDIA_TYPE_VALUE);
    public static final String GFF_MEDIA_TYPE_VALUE = "text/plain;format=gff";
    public static final MediaType GFF_MEDIA_TYPE = valueOf(GFF_MEDIA_TYPE_VALUE);
    public static final String OBO_MEDIA_TYPE_VALUE = "text/plain;format=obo";
    public static final MediaType OBO_MEDIA_TYPE = valueOf(OBO_MEDIA_TYPE_VALUE);
    public static final String RDF_MEDIA_TYPE_VALUE = "application/rdf+xml";
    public static final MediaType RDF_MEDIA_TYPE = valueOf(RDF_MEDIA_TYPE_VALUE);
    public static final String HDF5_MEDIA_TYPE_VALUE = "application/x-hdf5";
    public static final MediaType HDF5_MEDIA_TYPE = valueOf(HDF5_MEDIA_TYPE_VALUE);
    public static final String TURTLE_MEDIA_TYPE_VALUE = "text/turtle";
    public static final MediaType TURTLE_MEDIA_TYPE = valueOf(TURTLE_MEDIA_TYPE_VALUE);
    public static final String N_TRIPLES_MEDIA_TYPE_VALUE = "application/n-triples";
    public static final MediaType N_TRIPLES_MEDIA_TYPE = valueOf(N_TRIPLES_MEDIA_TYPE_VALUE);
    public static final String MARKDOWN_MEDIA_TYPE_VALUE = TEXT_MARKDOWN_VALUE;
    public static final MediaType MARKDOWN_MEDIA_TYPE = MediaType.TEXT_MARKDOWN;
    public static final String UNKNOWN_MEDIA_TYPE_VALUE = "unknown" + "/unknown";
    public static final MediaType UNKNOWN_MEDIA_TYPE = valueOf(UNKNOWN_MEDIA_TYPE_VALUE);

    public static final Collection<MediaType> ALL_TYPES =
            asList(
                    FF_MEDIA_TYPE,
                    LIST_MEDIA_TYPE,
                    TSV_MEDIA_TYPE,
                    XLS_MEDIA_TYPE,
                    FASTA_MEDIA_TYPE,
                    GFF_MEDIA_TYPE,
                    MARKDOWN_MEDIA_TYPE,
                    OBO_MEDIA_TYPE,
                    RDF_MEDIA_TYPE,
                    HDF5_MEDIA_TYPE,
                    TURTLE_MEDIA_TYPE,
                    N_TRIPLES_MEDIA_TYPE,
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_XML);

    private static final String FORMAT_PARAMETER = "format";
    private static HashBiMap<MediaType, String> mediaTypeExtensionMap = HashBiMap.create();

    static {
        mediaTypeExtensionMap.put(FF_MEDIA_TYPE, "txt");
        mediaTypeExtensionMap.put(XLS_MEDIA_TYPE, "xlsx");
        mediaTypeExtensionMap.put(RDF_MEDIA_TYPE, "rdf");
        mediaTypeExtensionMap.put(TURTLE_MEDIA_TYPE, "ttl");
        mediaTypeExtensionMap.put(N_TRIPLES_MEDIA_TYPE, "nt");
        mediaTypeExtensionMap.put(MARKDOWN_MEDIA_TYPE, "md");
        mediaTypeExtensionMap.put(HDF5_MEDIA_TYPE, "h5");

        ALL_TYPES.forEach(
                mediaType -> {
                    if (mediaType.getType().equals("text")
                            && mediaType.getSubtype().equals("plain")) {
                        mediaTypeExtensionMap.putIfAbsent(
                                mediaType, mediaType.getParameter(FORMAT_PARAMETER));
                    } else {
                        mediaTypeExtensionMap.putIfAbsent(mediaType, mediaType.getSubtype());
                    }
                });
    }

    public static MediaType valueOf(String typeValue) {
        return MediaType.valueOf(typeValue);
    }

    public static String getFileExtension(MediaType type) {
        return mediaTypeExtensionMap.getOrDefault(type, type.getSubtype());
    }

    public static MediaType getMediaTypeForFileExtension(String extension) {
        MediaType mediaType = mediaTypeExtensionMap.inverse().get(extension);
        if (Objects.nonNull(mediaType)) {
            return mediaType;
        } else {
            throw new IllegalArgumentException(
                    "Invalid extension or format supplied: " + extension);
        }
    }
}
