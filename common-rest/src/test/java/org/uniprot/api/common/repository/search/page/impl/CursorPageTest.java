package org.uniprot.api.common.repository.search.page.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/** @author lgonzales */
class CursorPageTest {

    private static final UriComponentsBuilder uriBuilder =
            UriComponentsBuilder.fromHttpUrl("http://localhost/test");

    @Test
    void firstPageOnlyShouldNotHaveNextLink() {
        CursorPage page = CursorPage.of(null, 10);
        assertNotNull(page);
        assertNull(page.getCursor());
        assertNull(page.getTotalElements());

        page.setTotalElements(10L);
        assertThat(page.getTotalElements(), CoreMatchers.is(10L));

        page.setNextCursor(
                "nextCursor"); // solr will always have a next cursor, even for single search
        // page...
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor, CoreMatchers.is("ahtuei6gxf98bwdgsi9e"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor, CoreMatchers.is("10,nextCursor"));

        assertFalse(page.getNextPageLink(uriBuilder).isPresent());
    }

    @Test
    void hasNextPage() {
        // create base36 cursor with the format: offset,cursorValue
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor, 10);
        assertNotNull(page);
        assertNotNull(page.getCursor());
        assertNull(page.getTotalElements());

        page.setTotalElements(21L);
        assertThat(page.getTotalElements(), CoreMatchers.is(21L));

        page.setNextCursor("TheNextCursor");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor, CoreMatchers.is("2yyr6dfwukrh5bpuykcvwdmk2"));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor, CoreMatchers.is("20,TheNextCursor"));

        assertTrue(page.getNextPageLink(uriBuilder).isPresent());
        String nextPageLink = page.getNextPageLink(uriBuilder).get();
        assertThat(
                nextPageLink,
                CoreMatchers.is("http://localhost/test?cursor=2yyr6dfwukrh5bpuykcvwdmk2&size=10"));
    }

    @Test
    void lastPage() {
        // create base36 cursor with the format: offset,cursorValue
        String currentCursor = new BigInteger("20,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor, 10);
        assertNotNull(page);
        assertNull(page.getTotalElements());
        assertNotNull(page.getCursor());

        page.setNextCursor("nextCursorItem");
        String nextCursor = page.getEncryptedNextCursor();
        assertThat(nextCursor, CoreMatchers.is("ljsr8c2ptesfv2u00i1d261sod"));

        page.setTotalElements(30L);
        assertThat(page.getTotalElements(), CoreMatchers.is(30L));

        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor, CoreMatchers.is("30,nextCursorItem"));

        assertFalse(page.getNextPageLink(uriBuilder).isPresent());
    }

    @Test
    void testCursorWithNext() {
        CursorPage page = CursorPage.of(null, 5, 15);
        assertNotNull(page);
        assertNull(page.getCursor());
        assertEquals(Long.valueOf(0), page.getOffset());
        assertEquals(Integer.valueOf(5), page.getPageSize());
        assertEquals(Long.valueOf(15), page.getTotalElements());
        String nextCursor = page.getEncryptedNextCursor();
        assertNotNull(nextCursor);
        String decryptedNextCursor = new String(new BigInteger(nextCursor, 36).toByteArray());
        assertThat(decryptedNextCursor, CoreMatchers.is("5,NEXT"));
        // next offset
        int nextOffset = CursorPage.getNextOffset(page);
        assertEquals(5, nextOffset);
    }

    @Test
    void testNextLinkURLEncodingWithPlusChar() {
        String inputQuery =
                "http://localhost/test?query=(family:\"major facilitator superfamily phosphate:H(+) symporter (TC 2.A.1.9) family\")";
        String encodedInputQuery =
                "http://localhost/test?query=%28family%3A%22major%20facilitator%20superfamily%20phosphate%3AH%28%2B%29%20symporter%20%28TC%202.A.1.9%29%20family%22%29";
        String addedCursorAndSize = "&cursor=3v3i3ublwto&size=10";

        validateEncodeAndDecodeNextPageLink(inputQuery, encodedInputQuery, addedCursorAndSize);
    }

    @Test
    void testNextLinkURLSimpleEncodingWithSpaces() {
        String inputQuery = "http://localhost/test?query=Protein name";
        String encodedInputQuery = "http://localhost/test?query=Protein%20name";
        String addedCursorAndSize = "&cursor=3v3i3ublwto&size=10";

        validateEncodeAndDecodeNextPageLink(inputQuery, encodedInputQuery, addedCursorAndSize);
    }

    @Test
    void testNextLinkURLWithBracketsChars() {
        String inputQuery = "http://localhost/test?query=(Protein name) AND length:[1 TO 10]";
        String encodedInputQuery =
                "http://localhost/test?query=%28Protein%20name%29%20AND%20length%3A%5B1%20TO%2010%5D";
        String addedCursorAndSize = "&cursor=3v3i3ublwto&size=10";

        validateEncodeAndDecodeNextPageLink(inputQuery, encodedInputQuery, addedCursorAndSize);
    }

    @Test
    void testNextLinkURLWithFacetsFieldsAndSort() {
        String inputQuery =
                "http://localhost/test?fields=accession,name,gene&facets=reviewed,annotation_score&sort=protein_name asc&query=(Protein name AND field:value)";
        String encodedInputQuery =
                "http://localhost/test?fields=accession,name,gene&facets=reviewed,annotation_score&sort=protein_name%20asc&query=%28Protein%20name%20AND%20field%3Avalue%29";
        String addedCursorAndSize = "&cursor=3v3i3ublwto&size=10";

        validateEncodeAndDecodeNextPageLink(inputQuery, encodedInputQuery, addedCursorAndSize);
    }

    @Test
    void testNextLinkURLgWithStarsChars() {
        String inputQuery = "http://localhost/test?query=*";
        String encodedInputQuery = "http://localhost/test?query=%2A";
        String addedCursorAndSize = "&cursor=3v3i3ublwto&size=10";

        validateEncodeAndDecodeNextPageLink(inputQuery, encodedInputQuery, addedCursorAndSize);
    }

    private void validateEncodeAndDecodeNextPageLink(
            String inputQuery, String encodedInputQuery, String addedCursorAndSize) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(inputQuery);
        String currentCursor = new BigInteger("10,currentCursor".getBytes()).toString(36);

        CursorPage page = CursorPage.of(currentCursor, 10);
        page.setTotalElements(21L);

        Optional<String> nextPageLink = page.getNextPageLink(uriBuilder);
        assertTrue(nextPageLink.isPresent());
        String link = nextPageLink.get();

        assertThat(link, CoreMatchers.is(encodedInputQuery + addedCursorAndSize));

        String decodeURL = UriUtils.decode(link, StandardCharsets.UTF_8);
        assertThat(decodeURL, CoreMatchers.is(inputQuery + addedCursorAndSize));
    }
}
