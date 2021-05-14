package org.uniprot.api.rest.output.header;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author lgonzales
 * @since 13/07/2020
 */
class HeaderFactoryTest {
    @Test
    void createHttpSearchHeaderThatRequiresCaching() {
        HttpHeaders result = HeaderFactory.createHttpSearchHeader(MediaType.APPLICATION_JSON);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
    }

    @Test
    void createHttpDownloadHeaderForStream() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
    }

    @Test
    void createHttpDownloadHeaderForDownload() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getQueryString()).thenReturn("gene:CDC7");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith(
                                "form-data; name=\"attachment\"; filename=\"uniprot-gene_CDC7-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithStar() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getQueryString()).thenReturn("*");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith("form-data; name=\"attachment\"; filename=\"uniprot-_-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadLongQuery() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getQueryString())
                .thenReturn("gene:INeedHereAVeryBigQuery OR gene:itAlsoNeedToBeBiggerThan60");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith(
                                "form-data; name=\"attachment\"; filename=\"uniprot-gene_INeedHereAVeryBigQuery_OR_gene_itAlsoNeedToBeBiggerThan-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithoutQuery() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getQueryString()).thenReturn("");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith("form-data; name=\"attachment\"; filename=\"uniprot-20"));
    }
}
