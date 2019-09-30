package org.uniprot.api.rest.pagination;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationEvent;
import org.uniprot.api.common.repository.search.page.Page;

/**
 * This class is an entity class that provide information for pagination event listener {@link
 * PaginatedResultsListener}.
 *
 * @author lgonzales
 */
public class PaginatedResultsEvent extends ApplicationEvent {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Page page;

    public PaginatedResultsEvent(
            final Object source,
            final HttpServletRequest request,
            final HttpServletResponse response,
            Page page) {
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
