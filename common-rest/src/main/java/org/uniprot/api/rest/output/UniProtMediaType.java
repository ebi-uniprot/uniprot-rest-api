package org.uniprot.api.rest.output;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Objects;

import org.springframework.http.MediaType;

import com.google.common.collect.HashBiMap;

public class UniProtMediaType {
    public static final String FF_MEDIA_TYPE_VALUE = "text/flatfile";
    public static final MediaType FF_MEDIA_TYPE = valueOf(FF_MEDIA_TYPE_VALUE);
    public static final String LIST_MEDIA_TYPE_VALUE = "text/list";
    public static final MediaType LIST_MEDIA_TYPE = valueOf(LIST_MEDIA_TYPE_VALUE);
    public static final String TSV_MEDIA_TYPE_VALUE = "text/tsv";
    public static final MediaType TSV_MEDIA_TYPE = valueOf(TSV_MEDIA_TYPE_VALUE);
    public static final String XLS_MEDIA_TYPE_VALUE = "application/vnd.ms-excel";
    public static final MediaType XLS_MEDIA_TYPE = valueOf(XLS_MEDIA_TYPE_VALUE);
    public static final String FASTA_MEDIA_TYPE_VALUE = "text/fasta";
    public static final MediaType FASTA_MEDIA_TYPE = valueOf(FASTA_MEDIA_TYPE_VALUE);
    public static final String GFF_MEDIA_TYPE_VALUE = "text/gff";
    public static final MediaType GFF_MEDIA_TYPE = valueOf(GFF_MEDIA_TYPE_VALUE);
    public static final String OBO_MEDIA_TYPE_VALUE = "text/obo";
    public static final MediaType OBO_MEDIA_TYPE = valueOf(OBO_MEDIA_TYPE_VALUE);
    public static final String RDF_MEDIA_TYPE_VALUE = "application/rdf+xml";
    public static final MediaType RDF_MEDIA_TYPE = valueOf(RDF_MEDIA_TYPE_VALUE);

    public static final Collection<MediaType> ALL_TYPES =
            asList(
                    FF_MEDIA_TYPE,
                    LIST_MEDIA_TYPE,
                    TSV_MEDIA_TYPE,
                    XLS_MEDIA_TYPE,
                    FASTA_MEDIA_TYPE,
                    GFF_MEDIA_TYPE,
                    OBO_MEDIA_TYPE,
                    RDF_MEDIA_TYPE);

    private static HashBiMap<MediaType, String> mediaTypeExtensionMap = HashBiMap.create();

    static {
        mediaTypeExtensionMap.put(FF_MEDIA_TYPE, "txt");
        mediaTypeExtensionMap.put(XLS_MEDIA_TYPE, "xlsx");
        mediaTypeExtensionMap.put(RDF_MEDIA_TYPE, "rdf");

        ALL_TYPES.forEach(
                mediaType -> mediaTypeExtensionMap.putIfAbsent(mediaType, mediaType.getSubtype()));
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
