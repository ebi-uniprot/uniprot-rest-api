package org.uniprot.api.rest.output.header;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author lgonzales
 * @since 13/07/2020
 */
class HeaderFactoryTest {

    @Test
    void createHttpSearchHeader() {
        HttpHeaders result = HeaderFactory.createHttpSearchHeader(MediaType.APPLICATION_JSON);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
    }

    @Test
    void createHttpDownloadHeaderForStream() {
        MessageConverterContext context = Mockito.mock(MessageConverterContext.class);
        Mockito.when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(context.isDownloadContentDispositionHeader()).thenReturn(false);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
    }

    @Test
    void createHttpDownloadHeaderForDownload() {
        MessageConverterContext context = Mockito.mock(MessageConverterContext.class);
        Mockito.when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        Mockito.when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn("gene:CDC7");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(result.getFirst("Content-Disposition").startsWith("form-data; name=\"attachment\"; filename=\"uniprot-gene_CDC7-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithStar() {
        MessageConverterContext context = Mockito.mock(MessageConverterContext.class);
        Mockito.when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        Mockito.when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn("*");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(result.getFirst("Content-Disposition").startsWith("form-data; name=\"attachment\"; filename=\"uniprot-_-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadLongQuery() {
        MessageConverterContext context = Mockito.mock(MessageConverterContext.class);
        Mockito.when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        Mockito.when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn("gene:INeedHereAVeryBigQuery OR gene:itAlsoNeedToBeBiggerThan60");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(result.getFirst("Content-Disposition").startsWith("form-data; name=\"attachment\"; filename=\"uniprot-gene_INeedHereAVeryBigQuery_OR_gene_itAlsoNeedToBeBiggerThan-"));
    }

    @Test
    void createHttpDownloadHeaderForDownloadWithoutQuery() {
        MessageConverterContext context = Mockito.mock(MessageConverterContext.class);
        Mockito.when(context.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(context.isDownloadContentDispositionHeader()).thenReturn(true);
        Mockito.when(context.getFileType()).thenReturn(FileType.FILE);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getQueryString()).thenReturn("");
        HttpHeaders result = HeaderFactory.createHttpDownloadHeader(context, request);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("application/json", result.getFirst("Content-Type"));
        assertEquals("Accept", result.getFirst("Vary"));
        assertNotNull(result.getFirst("Content-Disposition"));
        assertTrue(result.getFirst("Content-Disposition").startsWith("form-data; name=\"attachment\"; filename=\"uniprot-20"));
    }
}