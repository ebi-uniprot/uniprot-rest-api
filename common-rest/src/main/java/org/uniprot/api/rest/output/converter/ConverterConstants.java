package org.uniprot.api.rest.output.converter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class ConverterConstants {
    public static final String XML_DECLARATION =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"  standalone=\"no\" ?>\n";
    public static final String UNIPROTKB_XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniprot";

    public static final String UNIPROTKB_XML_SCHEMA =
            "<uniprot xmlns=\"http://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniprot http://www.uniprot.org/docs/uniprot.xsd\">\n";

    public static final String COPYRIGHT_TAG =
            "<copyright>\n"
                    + "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                    + "</copyright>\n";
    public static final String UNIPROTKB_XML_CLOSE_TAG = "</uniprot>";

    public static final String UNIPARC_XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniparc";
    public static final String UNIPARC_CROSS_REFERENCE_XML_CONTEXT = "org.uniprot.core.xml.jaxb.dbreference";

    public static final String UNIPARC_XML_SCHEMA =
            "<uniparc xmlns=\"http://uniprot.org/uniparc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniparc http://www.uniprot.org/docs/uniparc.xsd\">\n";

    public static final String UNIPARC_CROSS_REFERENCE_XML_SCHEMA =
            "<dbReferences xmlns=\"http://uniprot.org/dbReference\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/dbReference http://www.uniprot.org/docs/dbReference.xsd\">\n";

    public static final String UNIPARC_XML_CLOSE_TAG = "</uniparc>";
    public static final String UNIPARC_CROSS_REFERENCE_XML_CLOSE_TAG = "</dbReferences>";

    public static final String UNIREF_XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniref";
    public static final String UNIREF_XML_SCHEMA =
            "<UniRef xmlns=\"http://uniprot.org/uniref\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://uniprot.org/uniref http://www.uniprot.org/docs/uniref.xsd\"";

    public static final String UNIREF_XML_CLOSE_TAG = "\n</UniRef>";

    private ConverterConstants() {}
}
