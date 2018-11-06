package uk.ac.ebi.uniprot.rest.pagination;

import org.springframework.context.ApplicationEvent;
import uk.ac.ebi.uniprot.common.repository.search.page.Page;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is an entity class that provide information for pagination event listener
 * {@link PaginatedResultsListener}.
 *
 * @author lgonzales
 */
public class PaginatedResultsEvent extends ApplicationEvent {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Page page;

    public PaginatedResultsEvent(final Object source, final HttpServletRequest request, final HttpServletResponse response, Page page) {
        super(source);
        this.request = request;
        this.response = response;
        this.page = page;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public Page getPage() {
        return page;
    }

}
