package uk.ac.ebi.uniprot.common.repository.search.page.impl;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author lgonzales
 */
class CursorPageTest {

    private static UriComponentsBuilder uriBuilder =  UriComponentsBuilder.fromHttpUrl("http://localhost/test");

    @Test
    void firstPageOnlyShouldNotHaveNextLink() {
        CursorPage page = CursorPage.of(null,10);
        assertNotNull(page);
        assertNull(page.getCursor());
        assertNull(page.getTotalElements());

        page.setTotalElements(10L);
        assertThat(page.getTotalElements(),CoreMatchers.is(10L));

        page.setNextCursor("nextCursor"); // solr will always have a next cursor, even for single search page...
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor,CoreMatchers.is("ahtuei6gxf98bwdgsi9e"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor,CoreMatchers.is("10,nextCursor"));

        assertFalse(page.getNextPageLink(uriBuilder).isPresent());
    }


    @Test
    void hasNextPage() {
        // create base36 cursor with the format: offset,cursorValue
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor,10);
        assertNotNull(page);
        assertNotNull(page.getCursor());
        assertNull(page.getTotalElements());

        page.setTotalElements(21L);
        assertThat(page.getTotalElements(),CoreMatchers.is(21L));

        page.setNextCursor("nextCursor");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor,CoreMatchers.is("apidd3vzjype5ypqugz6"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor,CoreMatchers.is("20,nextCursor"));

        assertTrue(page.getNextPageLink(uriBuilder).isPresent());
        String nextPageLink = page.getNextPageLink(uriBuilder).get();
        assertThat(nextPageLink,CoreMatchers.is("http://localhost/test?cursor=apidd3vzjype5ypqugz6&size=10"));
    }


    @Test
    void lastPage() {
        // create base36 cursor with the format: offset,cursorValue
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor,10);
        assertNotNull(page);
        assertNotNull(page.getCursor());
        assertNull(page.getTotalElements());

        page.setTotalElements(20L);
        assertThat(page.getTotalElements(),CoreMatchers.is(20L));

        page.setNextCursor("nextCursor");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor,CoreMatchers.is("apidd3vzjype5ypqugz6"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor,CoreMatchers.is("20,nextCursor"));

        assertFalse(page.getNextPageLink(uriBuilder).isPresent());
    }

}