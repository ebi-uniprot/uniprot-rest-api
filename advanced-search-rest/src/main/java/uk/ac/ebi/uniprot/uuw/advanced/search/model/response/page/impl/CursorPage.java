package uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.Page;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * This class implements a cursor page with string nextCursor, previousCursor, pageSize  and totalElements capabilities
 * to navigate over result.
 *
 * @author lgonzales
 */
public class CursorPage implements Page {
    private static final Logger LOGGER = LoggerFactory.getLogger(CursorPage.class);

    private static final String CURSOR_PARAM_NAME = "cursor";
    private static final String SIZE_PARAM_NAME = "size";
    private static final String DELIMITER = ",";

    private final Stack<String> cursorStack;
    private String cursor;
    private String nextCursor;
    private Integer pageSize;
    private Long totalElements;

    private CursorPage(Stack<String> cursorStack, Integer pageSize){
        this.cursorStack = cursorStack;
        if(!cursorStack.isEmpty()) {
            this.cursor = cursorStack.peek();
        }
        this.pageSize = pageSize;
    }

    /**
     * Create and initialize CursorPage object.
     *
     * @param cursor current cursor received from request
     * @param pageSize current page size
     * @return CursorPage object
     */
    public static CursorPage of(String cursor, Integer pageSize){
        Stack<String> cursorStack = new Stack<>();
        if(cursor != null && !cursor.isEmpty()){
            String stringCursor = CursorEncryptor.decryptCursor(cursor);

            cursorStack.addAll(Arrays.asList(stringCursor.split(DELIMITER)));
        }
        return new CursorPage(cursorStack,pageSize);
    }

    /**
     *  if has next page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return next page link URL
     */
    @Override
    public Optional<String> getNextPageLink(UriComponentsBuilder uriBuilder) {
        Optional<String> nextPageLink = Optional.empty();
        if(hasNextPage()) {
            cursorStack.push(nextCursor);
            String nextPageCursor = cursorStack.stream().collect(Collectors.joining(DELIMITER));
            nextPageCursor = CursorEncryptor.encryptCursor(nextPageCursor);

            cursorStack.pop();

            uriBuilder.replaceQueryParam(CURSOR_PARAM_NAME, nextPageCursor);
            uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, pageSize);
            nextPageLink = Optional.of(uriBuilder.build().encode().toUriString());
        }
        return nextPageLink;
    }

    /**
     *  if has previous page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return previous page link URL
     */
    @Override
    public Optional<String> getPreviousPageLink(UriComponentsBuilder uriBuilder) {
        Optional<String> previousPageLink = Optional.empty();
        if(hasPreviousPage()) {
            if(isFirstPage()){
                previousPageLink = Optional.of(uriBuilder.build().encode().toUriString());
            }else {
                cursorStack.pop();
                String previousPageCursor = cursorStack.stream().collect(Collectors.joining(DELIMITER));

                previousPageCursor = CursorEncryptor.encryptCursor(previousPageCursor);

                uriBuilder.replaceQueryParam(CURSOR_PARAM_NAME, previousPageCursor);
                uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, pageSize);
                previousPageLink = Optional.of(uriBuilder.build().encode().toUriString());

                cursorStack.push(this.cursor);
            }
        }
        return previousPageLink;
    }

    public String getCursor(){
        return this.cursor;
    }

    public void setNextCursor(String nextCursor){
        this.nextCursor = nextCursor;
    }

    public void setTotalElements(Long totalElements){
        this.totalElements = totalElements;
    }

    @Override
    public Long getTotalElements() {
        return totalElements;
    }

    /**
     * Check if should have next page link
     *
     * @return true if numberOfPages * pageSize <= totalElements
     */
    private boolean hasNextPage() {
        int numberOfPages = this.cursorStack.size() + 1;
        return (numberOfPages * pageSize)  <= totalElements;
    }

    /**
     * Check if should have previous page link
     *
     * @return true if there is at least current cursor is in the cursor stack
     */
    private boolean hasPreviousPage() {
        return cursorStack.size() > 0;
    }


    /**
     * Check if should have previous page link
     *
     * @return true if there is at current cursor in the cursor stack
     */
    private boolean isFirstPage() {
        return cursorStack.size() == 1;
    }

    /**
     * This class is responsible to encrypt and decrypter Cursor page cursor
     *
     */
    private static class CursorEncryptor {

        /**
         * The encrypt compress String byte array and convert it to hexadecimal value
         *
         * @param valueToEnc value to be encrypted
         * @return encrypted value
         */
        private static String encryptCursor(String valueToEnc) {
            String encrypted = "";

            // convert string to byte array
            byte[] input = valueToEnc.getBytes();

            //BEGIN: compress byte array
            Deflater compressor = new Deflater();
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                compressor.setInput(input);
                compressor.finish();
                byte[] buffer = new byte[1024];
                int count = 0;
                while (!compressor.finished()) {
                    count = compressor.deflate(buffer);
                    stream.write(buffer, 0, count);
                }
                stream.flush();
                // convert it to hexadecimal
                encrypted = new BigInteger(stream.toByteArray()).toString(16);
            } catch (IOException e) {
                LOGGER.warn("Error compressing page cursor",e);
            } finally {
                compressor.end();
            }
            //END: compress byte array
            return encrypted;
        }

        /**
         * The decrypt convert the hexadecimal to byte array and decompress it.
         *
         * @param encryptedValue encrypted value
         * @return decrypted value
         */
        private static String decryptCursor(String encryptedValue) {
            String encrypted = "";

            // convert to hexadecimal value to byte array
            byte[] decodedValue = new BigInteger(encryptedValue, 16).toByteArray();

            // BEGIN: decompress byte array to original string
            Inflater decompressor = new Inflater();
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                decompressor.setInput(decodedValue);
                final byte[] buf = new byte[1024];
                while (!decompressor.finished()) {
                    int count = 0;
                    try {
                        count = decompressor.inflate(buf);
                    } catch (DataFormatException e) {
                        e.printStackTrace();
                    }
                    stream.write(buf, 0, count);
                }
                stream.flush();
                encrypted = new String(stream.toByteArray());
            } catch (IOException e) {
                LOGGER.warn("Error decompressing page cursor",e);
            } finally {
                decompressor.end();
            }
            // END: decompress byte array to original string

            return encrypted;
        }
    }
}
