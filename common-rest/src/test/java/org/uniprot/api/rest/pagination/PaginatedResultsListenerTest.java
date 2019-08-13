package org.uniprot.api.rest.pagination;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.rest.pagination.PaginatedResultsListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author lgonzales
 */
class PaginatedResultsListenerTest {


    @Test
    void onApplicationEventHasNextPage() {
        // given
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));
        HttpServletResponse response = new MockHttpServletResponse();
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);
        CursorPage page = CursorPage.of(currentCursor,10);
        page.setNextCursor("nextCursor");
        page.setTotalElements(120L);

        PaginatedResultsEvent event = new PaginatedResultsEvent(this,request,response,page);
        PaginatedResultsListener listener = new PaginatedResultsListener();

        //when
        listener.onApplicationEvent(event);

        //then
        assertNotNull(response.getHeader("X-TotalRecords"));
        assertEquals("120",response.getHeader("X-TotalRecords"));

        assertNotNull(response.getHeader("Link"));
        String expectedNextLink = "<https://localhost/test?cursor=apidd3vzjype5ypqugz6&size=10>; rel=\"next\"";
        assertEquals(expectedNextLink,response.getHeader("Link"));
    }

    @Test
    void onApplicationEventWithoutNextPage() {
        // given
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);
        CursorPage page = CursorPage.of(currentCursor,10);
        page.setTotalElements(12L);
        page.setNextCursor("nextCursor");

        HttpServletResponse response = new MockHttpServletResponse();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));

        PaginatedResultsEvent event = new PaginatedResultsEvent(this,request,response,page);
        PaginatedResultsListener listener = new PaginatedResultsListener();

        //when
        listener.onApplicationEvent(event);

        //then
        assertNull(response.getHeader("Link"));

        assertNotNull(response.getHeader("X-TotalRecords"));
        assertEquals("12",response.getHeader("X-TotalRecords"));

    }
}