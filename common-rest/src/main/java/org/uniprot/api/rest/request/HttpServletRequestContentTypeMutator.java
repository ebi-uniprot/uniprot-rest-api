package org.uniprot.api.rest.request;

import static org.springframework.http.MediaType.*;
import static org.uniprot.api.rest.output.UniProtMediaType.UNKNOWN_MEDIA_TYPE_VALUE;
import static org.uniprot.core.util.Utils.notNullNotEmpty;
import static org.uniprot.core.util.Utils.nullOrEmpty;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.core.util.Utils;

/**
 * A helper class that mutates an {@link HttpServletRequest} based on its values, and if necessary
 * sets the request's content type in the 'Accept' header.
 *
 * <p>Created 03/12/2019
 *
 * @author Edd
 */
@Slf4j
public class HttpServletRequestContentTypeMutator {
    public static final String ERROR_MESSAGE_ATTRIBUTE =
            "org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.errorMessageAttribute";
    public static final String FORMAT = "format";
    public static final String COMPRESSED = "compressed";
    private static final Set<String> VALID_EXTENSIONS =
            UniProtMediaType.ALL_TYPES.stream()
                    .map(UniProtMediaType::getFileExtension)
                    .collect(Collectors.toSet());
    private static final Pattern BROWSER_PATTERN =
            Pattern.compile("Mozilla|AppleWebKit|Edg|OPR|Chrome|Vivaldi");
    final Map<String, Collection<MediaType>> resourcePath2MediaTypes = new HashMap<>();
    final List<String> resourcePath2MediaTypesKeys = new ArrayList<>();

    public HttpServletRequestContentTypeMutator(
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        initResourcePath2MediaTypesMap(requestMappingHandlerMapping);
    }

    /**
     * This method gets all known paths and orders them so that paths containing
     * {@code @PathVariable}s appear last. This is important so that when finding matching paths in
     * {@link HttpServletRequestContentTypeMutator#getMatchingPathPattern(String)} will return
     * preferably a path that has no path (if two match), alternatively it will match the one with a
     * path variable. E.g,. Given [1] /a/b/{c} and [2]/a/b/resource, a request to /a/b/resource will
     * match [2]; on the other hand, a request to /a/b/resource2 would match [1].
     */
    static void orderKeysSoPathVariablesLast(List<String> pathVariables) {
        Comparator<String> stringComparator =
                Comparator.<String>naturalOrder()
                        .thenComparingLong(path -> path.chars().filter(c -> c == '{').count());
        pathVariables.sort(stringComparator);
    }

    public static boolean isBrowserAsFarAsWeKnow(String userAgent) {
        return notNullNotEmpty(userAgent) && BROWSER_PATTERN.matcher(userAgent).find();
    }

    public void mutate(MutableHttpServletRequest request) {
        mutateAcceptHeader(request);
        mutateAcceptEncodingHeader(request);
    }

    private void mutateAcceptHeader(MutableHttpServletRequest request) {
        Collection<MediaType> validMediaTypes = getValidMediaTypesForPath(request);
        if (validMediaTypes.isEmpty()) {
            return;
        }

        ExtensionValidationResult result = checkExtensionIsKnown(request);

        if (notNullNotEmpty(result.getExtensionUsed())) {
            // an known extension was used
            handleRequestedExtension(
                    request, validMediaTypes, result.getExtensionUsed(), result.isEntryResource());
        } else {
            if (notNullNotEmpty(result.getUnvalidatedRequestFormat())) {
                // format parameter is not known
                handleMediaTypeNotAcceptable(request, result.getUnvalidatedRequestFormat());
            } else {
                String requestedAcceptHeader = request.getHeader(HttpHeaders.ACCEPT);
                String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                // for empty accept headers, or browsers, use JSON
                if (nullOrEmpty(requestedAcceptHeader)
                        || requestAllowsAnyMediaType(requestedAcceptHeader)
                        || isBrowserAsFarAsWeKnow(userAgent)) {
                    request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE);
                } else {
                    if (!validMediaTypes.contains(parseMediaType(requestedAcceptHeader))) {
                        // if accept header is not valid for this request path
                        handleMediaTypeNotAcceptable(request, requestedAcceptHeader);
                    }
                }
            }
        }
    }

    private void mutateAcceptEncodingHeader(MutableHttpServletRequest request) {
        String compressed = request.getParameter(COMPRESSED);
        if ("true".equalsIgnoreCase(compressed)) {
            request.addHeader(HttpHeaders.ACCEPT_ENCODING, FileType.GZIP.getFileType());
        }
    }

    void initResourcePath2MediaTypesMap(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        requestMappingHandlerMapping
                .getHandlerMethods()
                .keySet()
                .forEach(
                        mappingInfo ->
                                mappingInfo
                                        .getPatternsCondition()
                                        // for every resource path
                                        .getPatterns()
                                        .forEach(
                                                pattern ->
                                                        // .. update map with: resource path ->
                                                        // its valid mediatypes
                                                        resourcePath2MediaTypes.put(
                                                                pattern,
                                                                mappingInfo
                                                                        .getProducesCondition()
                                                                        .getProducibleMediaTypes())));

        resourcePath2MediaTypesKeys.addAll(resourcePath2MediaTypes.keySet());
        orderKeysSoPathVariablesLast(resourcePath2MediaTypesKeys);
    }

    String getMatchingPathPattern(String requestURI) {
        String[] requestURLParts = requestURI.split("/");
        for (String pathPattern : resourcePath2MediaTypesKeys) {
            String[] pathPatternParts = pathPattern.split("/");

            if (pathPatternMatchesRequestURL(pathPatternParts, requestURLParts)) {
                return pathPattern;
            }
        }

        return null;
    }

    private static ExtensionValidationResult checkExtensionIsKnown(
            MutableHttpServletRequest request) {
        ExtensionValidationResult.ExtensionValidationResultBuilder resultBuilder =
                ExtensionValidationResult.builder();

        String requestURL = request.getRequestURL().toString();
        String unvalidatedRequestedFormat = request.getParameter(FORMAT);
        resultBuilder.unvalidatedRequestFormat(unvalidatedRequestedFormat);
        String extensionUsed = null;
        for (String validExtension : VALID_EXTENSIONS) {
            // check endsWith so we can handle tricky cases like, /a/XXX.1.txt for resource /a/XXX.1
            if (requestURL.endsWith("." + validExtension)) {
                resultBuilder.isEntryResource(true);
                extensionUsed = validExtension;
            } else {
                if (notNullNotEmpty(unvalidatedRequestedFormat)
                        && unvalidatedRequestedFormat.equals(validExtension)) {
                    extensionUsed = unvalidatedRequestedFormat;
                }
            }

            if (notNullNotEmpty(extensionUsed)) {
                // extension has been identified, so stop looping
                break;
            }
        }

        resultBuilder.extensionUsed(extensionUsed);
        return resultBuilder.build();
    }

    private static void handleRequestedExtension(
            MutableHttpServletRequest request,
            Collection<MediaType> validMediaTypesForPath,
            String extensionUsed,
            boolean isEntryResource) {

        MediaType mediaTypeForFileExtension =
                UniProtMediaType.getMediaTypeForFileExtension(extensionUsed);
        // user provided extension refers to a valid mediatype for this path
        if (validMediaTypesForPath.contains(mediaTypeForFileExtension)) {
            request.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());

            if (isEntryResource) {
                setURI(request, extensionUsed);
                setURL(request, extensionUsed);
                setServletPath(request, extensionUsed);
            }

        } else {
            handleMediaTypeNotAcceptable(request, validMediaTypesForPath, extensionUsed);
        }
    }

    private static void setServletPath(MutableHttpServletRequest request, String extension) {
        if (Utils.notNullNotEmpty(request.getServletPath())) {
            request.setServletPath(
                    request.getServletPath()
                            .substring(
                                    0,
                                    request.getServletPath().length() - (extension.length() + 1)));
        }
    }

    private static void handleMediaTypeNotAcceptable(
            MutableHttpServletRequest request,
            Collection<MediaType> validMediaTypesForPath,
            String extension) {
        String validMediaTypesMessage =
                "Requested media type/format not accepted, '"
                        + extension
                        + "'. Valid media types/formats for this end-point include: "
                        + validMediaTypesForPath.stream()
                                .map(MimeType::toString)
                                .collect(Collectors.joining(", "))
                        + ".";
        request.addHeader(HttpHeaders.ACCEPT, UNKNOWN_MEDIA_TYPE_VALUE);
        request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, validMediaTypesMessage);
    }

    private static void handleMediaTypeNotAcceptable(
            MutableHttpServletRequest request, String requestedFormat) {
        request.addHeader(HttpHeaders.ACCEPT, UNKNOWN_MEDIA_TYPE_VALUE);
        request.setAttribute(
                ERROR_MESSAGE_ATTRIBUTE,
                "Requested media type/format not accepted: '" + requestedFormat + "'.");
    }

    private static boolean pathPatternMatchesRequestURL(
            String[] pathPatternParts, String[] requestURLParts) {
        if (pathPatternParts.length == requestURLParts.length) {
            for (int i = 0; i < pathPatternParts.length; i++) {
                if (pathPatternParts[i].startsWith("{")
                        || pathPatternParts[i].equals(requestURLParts[i])) {
                    if (i == pathPatternParts.length - 1) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static void setURL(MutableHttpServletRequest request, String extension) {
        request.setRequestURL(
                request.getRequestURL()
                        .substring(0, request.getRequestURL().length() - (extension.length() + 1)));
    }

    private static void setURI(MutableHttpServletRequest request, String extension) {
        String uri =
                request.getRequestURI()
                        .substring(0, request.getRequestURI().length() - (extension.length() + 1));
        request.setRequestURI(uri);
    }

    private boolean requestAllowsAnyMediaType(String requestedAcceptHeader) {
        return requestedAcceptHeader.equals(ALL_VALUE);
    }

    private Collection<MediaType> getValidMediaTypesForPath(MutableHttpServletRequest request) {
        String actualPath = request.getRequestURI().substring(request.getContextPath().length());
        String matchingPath = getMatchingPathPattern(actualPath);
        return resourcePath2MediaTypes.getOrDefault(matchingPath, Collections.emptySet());
    }

    @Getter
    @Builder
    private static class ExtensionValidationResult {
        private final boolean isEntryResource;
        private final String extensionUsed;
        private final String unvalidatedRequestFormat;
    }
}
