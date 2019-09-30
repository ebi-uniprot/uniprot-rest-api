package org.uniprot.api.rest.output;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

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

    private static Map<MediaType, String> MEDIATYPE_EXTENSION_MAP = new HashMap<>();

    static {
        MEDIATYPE_EXTENSION_MAP.put(FF_MEDIA_TYPE, "txt");
        MEDIATYPE_EXTENSION_MAP.put(XLS_MEDIA_TYPE, "xlsx");
    };

    private static MediaType valueOf(String typeValue) {
        return MediaType.valueOf(typeValue);
    }

    public static String getFileExtension(MediaType type) {
        return MEDIATYPE_EXTENSION_MAP.getOrDefault(type, type.getSubtype());
    }
}
