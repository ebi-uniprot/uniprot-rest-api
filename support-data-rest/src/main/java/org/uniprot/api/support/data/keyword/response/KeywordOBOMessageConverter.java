package org.uniprot.api.support.data.keyword.response;

import org.obolibrary.oboformat.model.Frame;
import org.uniprot.api.rest.output.converter.AbstractOBOMessageConverter;
import org.uniprot.core.cv.keyword.KeywordEntry;

/**
 * @author sahmad
 * @created 21/01/2021
 */
public class KeywordOBOMessageConverter extends AbstractOBOMessageConverter<KeywordEntry> {

    private static final String KEYWORD_NAMESPACE = "uniprot:keywords";

    public KeywordOBOMessageConverter() {
        super(KeywordEntry.class);
    }

    @Override
    protected Frame getTermFrame(KeywordEntry entity) {
        Frame frame = new Frame(Frame.FrameType.TERM);
        return null;
    }

    @Override
    protected String getHeaderNamespace() {
        return KEYWORD_NAMESPACE;
    }
}
