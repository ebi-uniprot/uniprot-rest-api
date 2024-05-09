package org.uniprot.api.rest.output.context;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.core.util.Pair;

import lombok.Builder;
import lombok.Data;

/**
 * Created 07/09/18
 *
 * @author Edd
 */
@Builder
@Data
public class MessageConverterContext<T> {
    public static final FileType DEFAULT_FILE_TYPE = FileType.FILE;
    @Builder.Default private FileType fileType = DEFAULT_FILE_TYPE;
    private MediaType contentType;
    private Stream<T> entities;
    private Stream<String> entityIds;
    private MessageConverterContextFactory.Resource resource;
    private String fields;
    private Collection<Facet> facets;
    private Collection<TermInfo> matchedFields;
    private Collection<Suggestion> suggestions;
    private boolean entityOnly;
    private boolean downloadContentDispositionHeader;
    private ExtraOptions extraOptions;
    private Collection<ProblemPair> warnings;
    private boolean isLargeDownload;
    private String proteomeId;
    private boolean subsequence;
    /**
     * A map to store accessions along with their associated sequence ranges and a flag indicating
     * if they have been processed. Accessions can be repeated, allowing multiple sequence ranges
     * for the same accession. For example, "P12345[20-30], P12345[0-10]". The isProcessed flag is
     * set to true once a sequence range is handled.
     *
     * @see org.uniprot.api.uniprotkb.output.converter.UniProtKBFastaMessageConverter
     */
    private Map<String, List<Pair<String, Boolean>>> accessionSequenceRange;

    MessageConverterContext<T> asCopy() {
        return MessageConverterContext.<T>builder()
                .contentType(this.contentType)
                .entities(this.entities)
                .resource(this.resource)
                .fileType(this.fileType)
                .fields(this.fields)
                .entityIds(this.entityIds)
                .facets(this.facets)
                .matchedFields(this.matchedFields)
                .entityOnly(this.entityOnly)
                .downloadContentDispositionHeader(downloadContentDispositionHeader)
                .extraOptions(this.extraOptions)
                .suggestions(this.suggestions)
                .warnings(this.warnings)
                .isLargeDownload(this.isLargeDownload)
                .subsequence(subsequence)
                .accessionSequenceRange(this.accessionSequenceRange)
                .build();
    }
}
