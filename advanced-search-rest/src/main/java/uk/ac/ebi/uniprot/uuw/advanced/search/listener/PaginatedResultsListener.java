package uk.ac.ebi.uniprot.uuw.advanced.search.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.event.PaginatedResultsEvent;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.Page;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Optional;

/**
 * Event Listener Class responsible to build Link pagination header and also X-TotalRecords
 *
 * @author lgonzales
 */
@Component
public class PaginatedResultsListener implements ApplicationListener<PaginatedResultsEvent> {

    @Override
    public void onApplicationEvent(final PaginatedResultsEvent paginatedResultsEvent) {

        Page page = paginatedResultsEvent.getPage();
        HttpServletResponse response = paginatedResultsEvent.getResponse();
        HttpServletRequest request = paginatedResultsEvent.getRequest();
        UriComponentsBuilder uriBuilder = getUriComponentsBuilder(request);

        StringBuilder linkHeader = new StringBuilder();

        Optional<String> prevPageLink = page.getPreviousPageLink(uriBuilder);
        prevPageLink.ifPresent(s -> linkHeader.append(createLinkHeader(s, "prev")));

        Optional<String> nextPageLink = page.getNextPageLink(uriBuilder);
        nextPageLink.ifPresent(s -> linkHeader.append(createLinkHeader(s, "next")));

        if(!linkHeader.toString().isEmpty()) {
            response.addHeader("Link", linkHeader.toString());
        }

        Long totalRecords = page.getTotalElements();
        if(totalRecords > 0) {
            response.addHeader("X-TotalRecords", totalRecords.toString());
        }
    }

    /**
     * Creates a Link Header to be stored in the {@link javax.servlet.http.HttpServletResponse} to
     * provide Discoverability features to the user
     *
     * @param uri the base uri
     * @param rel the relative path
     *
     * @return the complete url
     */
    private static String createLinkHeader(final String uri, final String rel) {
        return "<" + uri + ">; rel=\"" + rel + "\"";
    }

    private UriComponentsBuilder getUriComponentsBuilder(HttpServletRequest request) {
        HashMap<String,String[]> params = new HashMap<>(request.getParameterMap());
        params.remove("cursor");

        UriComponentsBuilder uriBuilder =  UriComponentsBuilder.fromHttpUrl(String.valueOf(request.getRequestURL()));
        params.forEach(uriBuilder::queryParam);

        return uriBuilder;
    }
}
