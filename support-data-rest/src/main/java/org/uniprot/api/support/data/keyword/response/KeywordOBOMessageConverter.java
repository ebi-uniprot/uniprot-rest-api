package org.uniprot.api.support.data.keyword.response;

import java.util.Collections;
import java.util.Objects;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.uniprot.api.rest.output.converter.AbstractOBOMessageConverter;
import org.uniprot.core.cv.go.GoTerm;
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
    protected Frame getTermFrame(KeywordEntry keywordEntry) {
        Frame frame = new Frame(Frame.FrameType.TERM);
        frame.setId(keywordEntry.getKeyword().getId());
        frame.addClause(getIdClause(keywordEntry));
        frame.addClause(getNameClause(keywordEntry));

        if (Objects.nonNull(keywordEntry.getCategory())) {
            frame.addClause(getRelationship(keywordEntry));
        }
        for (String syn : keywordEntry.getSynonyms()) {
            frame.addClause(getSynonymClause(syn));
        }

        frame.addClause(getDefClause(keywordEntry));
        // add xref
        for (GoTerm goTerm : keywordEntry.getGeneOntologies()) {
            frame.addClause(getXRefClause(goTerm));
        }
        for (String site : keywordEntry.getSites()) {
            frame.addClause(getXRefClause(site));
        }

        for (KeywordEntry parent : keywordEntry.getParents()) {
            frame.addClause(getIsAClause(parent));
        }

        return frame;
    }

    @Override
    protected String getHeaderNamespace() {
        return KEYWORD_NAMESPACE;
    }

    @Override
    protected Frame getTypeDefStanza() {
        Frame headerFrame = new Frame(Frame.FrameType.TYPEDEF);
        headerFrame.setId("category");
        headerFrame.addClause(new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, "category"));
        headerFrame.addClause(new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, "category"));
        headerFrame.addClause(new Clause(OBOFormatConstants.OboFormatTag.TAG_IS_CYCLIC, "false"));
        return headerFrame;
    }

    private Clause getDefClause(KeywordEntry keywordEntry) {
        Clause clause =
                new Clause(OBOFormatConstants.OboFormatTag.TAG_DEF, keywordEntry.getDefinition());
        return clause;
    }

    private Clause getIdClause(KeywordEntry keywordEntry) {
        Clause clause =
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_ID, keywordEntry.getKeyword().getId());
        return clause;
    }

    private Clause getNameClause(KeywordEntry keywordEntry) {
        Clause clause =
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_NAME,
                        keywordEntry.getKeyword().getName());
        return clause;
    }

    private Clause getSynonymClause(String synonym) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, synonym);
        Xref xref = new Xref("UniProt");
        clause.setXrefs(Collections.singletonList(xref));
        return clause;
    }

    private Clause getRelationship(KeywordEntry keywordEntry) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_RELATIONSHIP, "category");
        clause.addValue(keywordEntry.getCategory().getId());
        return clause;
    }

    private Clause getXRefClause(GoTerm goTerm) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        Xref xref = new Xref(goTerm.getId());
        xref.setAnnotation(goTerm.getName());
        clause.setValues(Collections.singletonList(xref));
        return clause;
    }

    private Clause getXRefClause(String site) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        Xref xref = new Xref(site);
        clause.addValue(xref);
        return clause;
    }

    private Clause getIsAClause(KeywordEntry parent) {
        return new Clause(OBOFormatConstants.OboFormatTag.TAG_IS_A, parent.getKeyword().getId());
    }
}
