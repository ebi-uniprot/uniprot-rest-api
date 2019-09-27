package org.uniprot.api.common.repository.search.page.impl;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * @author lgonzales
 */
class CursorPageTest {

    private static final UriComponentsBuilder uriBuilder =  UriComponentsBuilder.fromHttpUrl("http://localhost/test");

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

        page.setNextCursor("TheNextCursor");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor,CoreMatchers.is("2yyr6dfwukrh5bpuykcvwdmk2"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor,CoreMatchers.is("20,TheNextCursor"));

        assertTrue(page.getNextPageLink(uriBuilder).isPresent());
        String nextPageLink = page.getNextPageLink(uriBuilder).get();
        assertThat(nextPageLink,CoreMatchers.is("http://localhost/test?cursor=2yyr6dfwukrh5bpuykcvwdmk2&size=10"));
    }


    @Test
    void lastPage() {
        // create base36 cursor with the format: offset,cursorValue
        String currentCursor = new BigInteger("20,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor,10);
        assertNotNull(page);
        assertNull(page.getTotalElements());
        assertNotNull(page.getCursor());

        page.setNextCursor("nextCursorItem");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor,CoreMatchers.is("ljsr8c2ptesfv2u00i1d261sod"));

        page.setTotalElements(30L);
        assertThat(page.getTotalElements(),CoreMatchers.is(30L));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor,CoreMatchers.is("30,nextCursorItem"));

        assertFalse(page.getNextPageLink(uriBuilder).isPresent());
    }

}