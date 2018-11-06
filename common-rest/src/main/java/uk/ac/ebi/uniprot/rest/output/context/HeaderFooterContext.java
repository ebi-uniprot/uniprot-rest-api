package uk.ac.ebi.uniprot.rest.output.context;

/**
 * Created 22/10/18
 *
 * @author Edd
 */
interface HeaderFooterContext {
    String getHeader();

    void setHeader(String header);

    String getFooter();

    void setFooter(String footer);
}
