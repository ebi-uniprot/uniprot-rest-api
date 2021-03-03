package org.uniprot.api.rest.output.converter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class ConverterConstants {
    public static final String UNIPROTKB_XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniprot";

    public static final String UNIPROTKB_XML_HEADER =
            "<uniprot xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/uniprot.xsd\">\n";
    public static final String UNIPROTKB_XML_FOOTER =
            "<copyright>\n"
                    + "Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                    + "</copyright>\n"
                    + "</uniprot>";

    public static final String UNIPARC_XML_CONTEXT = "org.uniprot.core.xml.jaxb.uniparc";

    public static final String UNIPARC_XML_HEADER =
            "<uniparc xmlns=\"https://uniprot.org/uniprot\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://uniprot.org/uniprot https://www.uniprot.org/docs/uniparc.xsd\">\n";

    public static final String UNIPARC_XML_FOOTER = "\n</uniparc>";

    private ConverterConstants() {}
}
