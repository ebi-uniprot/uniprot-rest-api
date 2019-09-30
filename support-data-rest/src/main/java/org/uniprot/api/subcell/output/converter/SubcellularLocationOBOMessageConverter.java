package org.uniprot.api.subcell.output.converter;

import java.util.Collections;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.uniprot.api.rest.output.converter.AbstractOBOMessagerConverter;
import org.uniprot.core.cv.keyword.GeneOntology;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 2019-08-29
 */
public class SubcellularLocationOBOMessageConverter
        extends AbstractOBOMessagerConverter<SubcellularLocationEntry> {
    private static final String SUBCELLULAR_LOCATION_NAMESPACE = "uniprot:locations";

    public SubcellularLocationOBOMessageConverter() {
        super(SubcellularLocationEntry.class);
    }

    @Override
    public Frame getTermFrame(SubcellularLocationEntry subcellularLocationEntry) {
        Frame frame = new Frame(Frame.FrameType.TERM);

        frame.setId(subcellularLocationEntry.getAccession());
        frame.addClause(getIdClause(subcellularLocationEntry));
        frame.addClause(getNameClause(subcellularLocationEntry));

        // add synonyms
        if (Utils.notEmpty(subcellularLocationEntry.getSynonyms())) {
            for (String syn : subcellularLocationEntry.getSynonyms()) {
                frame.addClause(getSynonymClause(syn));
            }
        }

        frame.addClause(getNameSpaceClause(subcellularLocationEntry));
        frame.addClause(getDefClause(subcellularLocationEntry));

        // add xref
        if (Utils.notEmpty(subcellularLocationEntry.getLinks())) {
            for (String xref : subcellularLocationEntry.getLinks()) {
                frame.addClause(getLinksXRefClause(xref));
            }
        }
        // add xref for GO terms
        if (Utils.notEmpty(subcellularLocationEntry.getGeneOntologies())) {
            for (GeneOntology go : subcellularLocationEntry.getGeneOntologies()) {
                frame.addClause(getGeneOntologyXRefClause(go));
            }
        }

        // add is a
        for (SubcellularLocationEntry isA : subcellularLocationEntry.getIsA()) {
            frame.addClause(getIsAClause(isA));
        }

        // add relationship
        for (SubcellularLocationEntry partOf : subcellularLocationEntry.getPartOf()) {
            frame.addClause(getRelationshipClause(partOf));
        }

        return frame;
    }

    private Clause getGeneOntologyXRefClause(GeneOntology go) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        Xref xref = new Xref(go.getGoId());
        xref.setAnnotation(go.getGoTerm());
        clause.setValue(xref);
        return clause;
    }

    @Override
    public String getHeaderNamespace() {
        return SUBCELLULAR_LOCATION_NAMESPACE;
    }

    private Clause getSynonymClause(String synonym) {
        Xref xref = new Xref("UniProt");
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, synonym);
        clause.setXrefs(Collections.singletonList(xref));
        return clause;
    }

    private Clause getLinksXRefClause(String link) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        clause.addValue(new Xref(link));
        return clause;
    }

    private Clause getNameSpaceClause(SubcellularLocationEntry subcellularLocationEntry) {
        String nameSpace =
                SUBCELLULAR_LOCATION_NAMESPACE
                        + ":"
                        + subcellularLocationEntry
                                .getCategory()
                                .toDisplayName()
                                .toLowerCase()
                                .replace(' ', '_');
        return new Clause(OBOFormatConstants.OboFormatTag.TAG_NAMESPACE, nameSpace);
    }

    private Clause getDefClause(SubcellularLocationEntry subcellularLocationEntry) {
        return new Clause(
                OBOFormatConstants.OboFormatTag.TAG_DEF, subcellularLocationEntry.getDefinition());
    }

    private Clause getIdClause(SubcellularLocationEntry subcellularLocationEntry) {
        return new Clause(
                OBOFormatConstants.OboFormatTag.TAG_ID, subcellularLocationEntry.getAccession());
    }

    private Clause getNameClause(SubcellularLocationEntry subcellularLocationEntry) {
        return new Clause(
                OBOFormatConstants.OboFormatTag.TAG_NAME, subcellularLocationEntry.getId());
    }

    private Clause getIsAClause(SubcellularLocationEntry isA) {
        return new Clause(OBOFormatConstants.OboFormatTag.TAG_IS_A, isA.getAccession());
    }

    private Clause getRelationshipClause(SubcellularLocationEntry partOf) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_RELATIONSHIP, "part_of");
        clause.addValue(partOf.getAccession());
        return clause;
    }
}
