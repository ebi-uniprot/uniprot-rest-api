package org.uniprot.api.disease.response.converter;

import java.util.*;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.uniprot.api.rest.output.converter.AbstractOBOMessagerConverter;
import org.uniprot.core.cv.disease.CrossReference;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.core.util.Utils;

public class DiseaseOBOMessageConverter extends AbstractOBOMessagerConverter<Disease> {
    private static final String DISEASE_NAMESPACE = "uniprot:diseases";

    public DiseaseOBOMessageConverter() {
        super(Disease.class);
    }

    @Override
    public Frame getTermFrame(Disease diseaseEntry) {
        Frame frame = new Frame(Frame.FrameType.TERM);

        frame.setId(diseaseEntry.getAccession());
        frame.addClause(getIdClause(diseaseEntry));
        frame.addClause(getNameClause(diseaseEntry));

        if (Utils.notNullOrEmpty(diseaseEntry.getAlternativeNames())) {
            for (String syn : diseaseEntry.getAlternativeNames()) {
                frame.addClause(getSynonymClause(syn));
            }
        }

        frame.addClause(getDefClause(diseaseEntry));

        // add xref
        for (CrossReference xref : diseaseEntry.getCrossReferences()) {
            frame.addClause(getXRefClause(xref));
        }

        return frame;
    }

    @Override
    public String getHeaderNamespace() {
        return DISEASE_NAMESPACE;
    }

    public Clause getSynonymClause(String synonym) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_SYNONYM, synonym);
        Xref xref = new Xref("UniProt");
        clause.setXrefs(Collections.singletonList(xref));
        return clause;
    }

    public Clause getDefClause(Disease diseaseEntry) {
        Clause clause =
                new Clause(OBOFormatConstants.OboFormatTag.TAG_DEF, diseaseEntry.getDefinition());
        return clause;
    }

    public Clause getIdClause(Disease disease) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_ID, disease.getAccession());
        return clause;
    }

    public Clause getNameClause(Disease disease) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_NAME, disease.getId());
        return clause;
    }

    public Clause getXRefClause(CrossReference crossRef) {
        Clause clause = new Clause(OBOFormatConstants.OboFormatTag.TAG_XREF);
        Xref xref = new Xref(crossRef.getDatabaseType() + ":" + crossRef.getId());
        if (Utils.notNullOrEmpty(crossRef.getProperties())) {
            xref.setAnnotation(crossRef.getProperties().get(0));
        }
        clause.setValues(Collections.singletonList(xref));
        return clause;
    }
}
