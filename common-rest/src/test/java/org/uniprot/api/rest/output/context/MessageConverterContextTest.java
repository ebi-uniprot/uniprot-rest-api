package org.uniprot.api.rest.output.context;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.uniprot.api.rest.output.context.MessageConverterContext.DEFAULT_FILE_TYPE;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.uniprot.api.common.repository.search.ExtraOptions;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.rest.output.FakePair;

/**
 * Created 08/11/18
 *
 * @author Edd
 */
class MessageConverterContextTest {
    @Test
    void canCreateUsingBuilder() {
        FileType fileType = FileType.FILE;
        MediaType contentType = MediaType.APPLICATION_JSON;
        Stream<String> entityStream = Stream.of("a", "b");
        Stream<String> entityIdStream = Stream.of("a.id", "b.id");
        List<Facet> facets = emptyList();
        List<TermInfo> matchedFields = emptyList();
        MessageConverterContextFactory.Resource resource =
                MessageConverterContextFactory.Resource.UNIPROTKB;
        String fields = "field1,field2";
        ExtraOptions extraOptions =
                ExtraOptions.builder()
                        .failedIds(List.of("id1"))
                        .suggestedId(FakePair.builder().from("fid2").to("tid2").build())
                        .obsoleteCount(5)
                        .build();
        List<ProblemPair> warnings = List.of(new ProblemPair(1, "msg"));
        List<Suggestion> suggestions = List.of(Suggestion.builder().build());

        MessageConverterContext<String> context =
                MessageConverterContext.<String>builder()
                        .fileType(fileType)
                        .contentType(contentType)
                        .entities(entityStream)
                        .entityIds(entityIdStream)
                        .facets(facets)
                        .matchedFields(matchedFields)
                        .fields(fields)
                        .resource(resource)
                        .subsequence(true)
                        .isLargeDownload(true)
                        .entityOnly(true)
                        .downloadContentDispositionHeader(true)
                        .extraOptions(extraOptions)
                        .warnings(warnings)
                        .suggestions(suggestions)
                        .build();

        assertThat(context.getContentType(), is(contentType));
        assertThat(context.getFileType(), is(fileType));
        assertThat(context.getEntities(), is(entityStream));
        assertThat(context.getEntityIds(), is(entityIdStream));
        assertThat(context.getFacets(), is(facets));
        assertThat(context.getMatchedFields(), is(matchedFields));
        assertThat(context.getFields(), is(fields));
        assertThat(context.getResource(), is(resource));
        assertThat(context.isSubsequence(), is(true));
        assertThat(context.isLargeDownload(), is(true));
        assertThat(context.isEntityOnly(), is(true));
        assertThat(context.isDownloadContentDispositionHeader(), is(true));
        assertThat(context.getExtraOptions(), is(extraOptions));
        assertThat(context.getSuggestions(), is(suggestions));
        assertThat(context.getWarnings(), is(warnings));
    }

    @Test
    void asCopyWorksAsExpected() {
        FileType fileType = FileType.FILE;
        MediaType contentType = MediaType.APPLICATION_JSON;
        Stream<String> entityStream = Stream.of("a", "b");
        Stream<String> entityIdStream = Stream.of("a.id", "b.id");
        List<Facet> facets = emptyList();
        List<TermInfo> matchedFIelds = emptyList();
        MessageConverterContextFactory.Resource resource =
                MessageConverterContextFactory.Resource.UNIPROTKB;
        String fields = "field1,field2";
        ExtraOptions extraOptions =
                ExtraOptions.builder()
                        .failedIds(List.of("id1"))
                        .suggestedId(FakePair.builder().from("fid2").to("tid2").build())
                        .obsoleteCount(10)
                        .build();
        List<ProblemPair> warnings = List.of(new ProblemPair(1, "msg"));
        List<Suggestion> suggestions = List.of(Suggestion.builder().build());

        MessageConverterContext<String> context =
                MessageConverterContext.<String>builder()
                        .fileType(fileType)
                        .contentType(contentType)
                        .entities(entityStream)
                        .entityIds(entityIdStream)
                        .facets(facets)
                        .matchedFields(matchedFIelds)
                        .fields(fields)
                        .resource(resource)
                        .subsequence(true)
                        .isLargeDownload(true)
                        .entityOnly(true)
                        .downloadContentDispositionHeader(true)
                        .extraOptions(extraOptions)
                        .warnings(warnings)
                        .suggestions(suggestions)
                        .build();

        MessageConverterContext<String> contextCopy = context.asCopy();

        assertThat(contextCopy.getContentType(), is(contentType));
        assertThat(contextCopy.getFileType(), is(fileType));
        assertThat(contextCopy.getEntities(), is(entityStream));
        assertThat(contextCopy.getEntityIds(), is(entityIdStream));
        assertThat(contextCopy.getFacets(), is(facets));
        assertThat(contextCopy.getMatchedFields(), is(matchedFIelds));
        assertThat(contextCopy.getFields(), is(fields));
        assertThat(contextCopy.getResource(), is(resource));
        assertThat(contextCopy.isSubsequence(), is(true));
        assertThat(context.isLargeDownload(), is(true));
        assertThat(context.isEntityOnly(), is(true));
        assertThat(context.isDownloadContentDispositionHeader(), is(true));
        assertThat(context.getExtraOptions(), is(extraOptions));
        assertThat(context.getSuggestions(), is(suggestions));
        assertThat(context.getWarnings(), is(warnings));
    }

    @Test
    void defaultFileTypeIsUsed() {
        MessageConverterContext<String> context = MessageConverterContext.<String>builder().build();

        assertThat(context.getFileType(), is(DEFAULT_FILE_TYPE));
    }
}
