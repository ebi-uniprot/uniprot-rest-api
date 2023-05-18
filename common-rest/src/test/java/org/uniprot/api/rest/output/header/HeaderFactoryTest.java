package org.uniprot.api.rest.output.header;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

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
        when(request.getParameter(Mockito.eq("query"))).thenReturn("(gene:P53)");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith("form-data; name=\"attachment\"; filename=\"_gene_P53_20"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithStar() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(Mockito.eq("query"))).thenReturn("*");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith("form-data; name=\"attachment\"; filename=\"_all_"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadLongQuery() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(Mockito.eq("query")))
                .thenReturn("gene:INeedHereAVeryBigQuery OR gene:itAlsoNeedToBeBiggerThan60");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith(
                                "form-data; name=\"attachment\"; filename=\"_gene_INeedHereAVeryBigQuery_OR_"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithoutQuery() {
        MessageConverterContext context = mock(MessageConverterContext.class);
        when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(Mockito.eq("query"))).thenReturn("");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(
                result.getFirst("Content-Disposition")
                        .startsWith("form-data; name=\"attachment\"; filename=\"_20"));
    }
}
