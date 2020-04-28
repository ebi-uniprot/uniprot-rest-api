package org.uniprot.api.rest.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.util.Utils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.createUnknownMediaTypeForFileExtension;

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
    private static final String FORMAT = "format";
    private static final String SEARCH = "/search";
    private static final String DOWNLOAD = "/download";
    private static final Pattern ENTRY_CONTEXT_PATH_PATTERN =
            Pattern.compile("^(/([\\w-]+/)*)([\\w-]+)$");
    private static final Pattern ENTRY_CONTEXT_PATH_MATCHER =
            // TODO: 03/04/20 test
            Pattern.compile("^(/[\\w-]+)*/[\\w-]+\\.([\\w-]+)$");
    private static final Set<String> ALLOWED_ACCEPT_HEADERS =
            UniProtMediaType.ALL_TYPES.stream().map(MimeType::toString).collect(Collectors.toSet());

    private HttpServletRequestContentTypeMutator() {}

    private static final Map<String, Collection<MediaType>> RESOURCE_PATH_2_MEDIA_TYPES =
            new HashMap<>();
    private static final Set<String> VALID_EXTENSIONS =
            UniProtMediaType.ALL_TYPES.stream()
                    .map(UniProtMediaType::getFileExtension)
                    .collect(Collectors.toSet());

    private static void initResourcePath2MediaTypesMap(
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        if (RESOURCE_PATH_2_MEDIA_TYPES.isEmpty()) {
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
                                                            RESOURCE_PATH_2_MEDIA_TYPES.put(
                                                                    pattern,
                                                                    mappingInfo
                                                                            .getProducesCondition()
                                                                            .getProducibleMediaTypes())));
        }
    }

    /*
    - create map, M <- resourcePaths -> List<MediaType>
    - create set, VEs <- valid extensions
    - for each ve : VEs
    -   if real url ends in ve
    -     VE <- ve
    -     P <- extractActualPathFrom(url)
    -     break
    - done
    - if VE is set
    -   VM <- M[P]
    -   if mediaTypeFor(VE) in VM
    -     set accept header to mediaTypeFor(VE)
    -   else
    -     throw exception for unknown mediatype requested for path, VE
    - else
    -   if accept header is empty, or, if user agent is a browser
    -     set default media type
    -   else
    -     (i.e., no extension/format was set, accept header is not empty, and user agent is not a browser)
    -     do nothing and allow framework to handle it
    */

    public static void mutate(
            MutableHttpServletRequest request,
            RequestMappingHandlerMapping requestMappingHandlerMapping)
            throws HttpMediaTypeNotAcceptableException {
        initResourcePath2MediaTypesMap(requestMappingHandlerMapping);

        String requestURL = request.getRequestURL().toString();
        String unvalidatedRequestedFormat = request.getParameter(FORMAT);
        String extensionUsed = null;
        boolean isEntryResource = false;
        for (String validExtension : VALID_EXTENSIONS) {
            // by checking endsWith, this means we can handle requests like, /a/XXX.1.txt for
            // resource /a/XXX.1
            if (requestURL.endsWith("." + validExtension)) {
                extensionUsed = validExtension;
                //                mutateEntryRequest2(request, extensionUsed);
                // register request mutator
//                String thing = extensionUsed;
                isEntryResource = true;
                //                request.registerAttributeMutator(
                //                        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                //                        req -> entryMutateOperation(req, thing));
                break;
            } else {
                if (Utils.notNullNotEmpty(unvalidatedRequestedFormat) && unvalidatedRequestedFormat.equals(validExtension)) {
                    extensionUsed = unvalidatedRequestedFormat;
                    break;
                }
            }
        }

        if (Utils.notNullNotEmpty(extensionUsed)) {
            String extension = extensionUsed;

            String matchingPath = getMatchingPathPattern(request.getRequestURI());
            Collection<MediaType> validMediaTypesForPath =
                    RESOURCE_PATH_2_MEDIA_TYPES.getOrDefault(matchingPath, Collections.emptySet());

            MediaType mediaTypeForFileExtension =
                    UniProtMediaType.getMediaTypeForFileExtension(extension);
            // user provided extension refers to a valid mediatype for this path
            if (validMediaTypesForPath.contains(mediaTypeForFileExtension)) {
                request.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());

                if (isEntryResource) {
                    setURI(request, extension);
                    setURL(request, extension);
                }

            } else {
                // throw exception for unknown mediatype requested for path
                throw new HttpMediaTypeNotAcceptableException(
                        // TODO: 24/04/2020 list the valid ones here
                        "Unknown requested extension'" + extension + "'");
            }
        } else {
            if (Utils.notNullNotEmpty(unvalidatedRequestedFormat)) {
                request.addHeader(HttpHeaders.ACCEPT, UniProtMediaType.createUnknownMediaTypeForFileExtension(unvalidatedRequestedFormat).toString());
//                throw new HttpMediaTypeNotAcceptableException(
//                         TODO: 24/04/2020 list the valid ones here
//                        "Unknown requested extension '" + unvalidatedRequestedFormat + "'");
            } else {
                String requestedAcceptHeader = request.getHeader(HttpHeaders.ACCEPT);
                String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                // for empty accept headers, or browsers, use JSON
                if (Utils.nullOrEmpty(requestedAcceptHeader) || isBrowserAsFarAsWeKnow(userAgent)) {
                    request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE);
                } else {
                    throw new HttpMediaTypeNotAcceptableException(
                            // TODO: 24/04/2020 list the valid ones here
                            "Unknown requested extension'" + requestedAcceptHeader + "'");
                }
            }
        }
    }

    // /a/b/{c}
    // /a/b/XX
    private static String getMatchingPathPattern(String requestURL) {
        Set<String> pathPatterns = RESOURCE_PATH_2_MEDIA_TYPES.keySet();
        String[] requestURLParts = requestURL.split("/");
        searchInt: for (String pathPattern : pathPatterns) {

            String[] pathPatternParts = pathPattern.split("/");
            if (pathPatternParts.length == requestURLParts.length) {
                for (int i = 0; i < pathPatternParts.length && i < requestURLParts.length; i++) {
                    if (pathPatternParts[i].startsWith("{")
                            || pathPatternParts[i].equals(requestURLParts[i])) {
                        if (i == pathPatternParts.length - 1) {
                            return pathPattern;
                        }
                    } else {
                        continue searchInt;
                    }
                }
            }
        }
        return null;
    }

    private static class UncheckedHttpMediaTypeNotAcceptableException extends RuntimeException {
        public UncheckedHttpMediaTypeNotAcceptableException(String message) {
            super(message);
        }
    }

    private static void handlePattern(MutableHttpServletRequest req, String extension) {
        // get the valid content types for this path
        String matchingPath =
                req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        Collection<MediaType> validMediaTypesForPath =
                RESOURCE_PATH_2_MEDIA_TYPES.get(matchingPath);

        MediaType mediaTypeForFileExtension =
                UniProtMediaType.getMediaTypeForFileExtension(extension);
        // user provided extension refers to a valid mediatype for this path
        if (validMediaTypesForPath.contains(mediaTypeForFileExtension)) {
            req.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());
        } else {
            // throw exception for unknown mediatype requested for path
            throw new UncheckedHttpMediaTypeNotAcceptableException(
                    // TODO: 24/04/2020 list the valid ones here
                    "Unknown requested extension'" + extension + "'");
        }
    }

    private static final Pattern BROWSER_PATTERN =
            Pattern.compile("Mozilla|AppleWebKit|Edg|OPR|Chrome|Vivaldi");

    private static boolean isBrowserAsFarAsWeKnow(String userAgent) {
        return Utils.notNullNotEmpty(userAgent) && BROWSER_PATTERN.matcher(userAgent).find();
    }

    private static List<String> getDefaultContentTypeForMapping(
            MutableHttpServletRequest request,
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        //        requestMappingHandlerMapping.getHandlerMethods().
        return null;
    }

    private static String extractContentTypeFromURL(MutableHttpServletRequest request) {
        String contentTypeFromExtension = extractContentTypeFromExtension(request);
        if (Utils.notNullNotEmpty(contentTypeFromExtension)) {
            return contentTypeFromExtension;
        } else {
            String contentTypeFromFormatParameter = extractContentTypeFromFormatParameter(request);
            if (Utils.notNullNotEmpty(contentTypeFromFormatParameter)) {
                return contentTypeFromFormatParameter;
            } else {
                return null;
            }
        }
    }

    private static String extractContentTypeFromFormatParameter(MutableHttpServletRequest request) {
        String format = request.getParameter(FORMAT);
        if ((request.getRequestURI().endsWith(DOWNLOAD) || request.getRequestURI().endsWith(SEARCH))
                && Utils.notNullNotEmpty(format)) {
            return format;
        } else {
            return null;
        }
    }

    private static String extractContentTypeFromExtension(MutableHttpServletRequest request) {
        Matcher entryContextMatcher = ENTRY_CONTEXT_PATH_MATCHER.matcher(request.getRequestURL());
        if (entryContextMatcher.matches()) {
            String entryPathVariable = entryContextMatcher.group(2);
            String entryId = entryContextMatcher.group(3);
            String extension = entryContextMatcher.group(4);

            setRealEntryId(request, entryPathVariable, entryId);
            setURI(request, extension);
            setURL(request, extension);

            return extension;
        } else {
            return null;
        }
    }

    public static void mutate(MutableHttpServletRequest request) {

        boolean mutated = mutateEntryRequest(request);
        mutated = mutated || mutateSearchOrDownloadRequest(request);
        addDefaultAcceptHeaderIfRequired(mutated, request);
    }

    private static void mutateEntryRequest2(MutableHttpServletRequest request, String extension) {
        // remove .extension from url, uri
        setURI(request, extension);
        setURL(request, extension);

        // add content type based on extension
        addContentTypeHeaderForFormat(request, extension);

        // get spring specific attributes of request, e.g., path variables mapped to their values
        Map<String, String> springRequestAttributes =
                (Map<String, String>)
                        request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        // if a path variable ends in the "." + extension, remove this ending, because its is to
        // indicate content type
        for (Map.Entry<String, String> attributeMapping : springRequestAttributes.entrySet()) {
            String value = attributeMapping.getValue();
            String dotExtension = "." + extension;
            if (value.endsWith(dotExtension)) {
                attributeMapping.setValue(value.substring(0, dotExtension.length()));
            }
        }
    }

    private static void entryMutateOperation(MutableHttpServletRequest request, String extension) {
        // remove .extension from url, uri
        //        setURI(request, extension);
        //        setURL(request, extension);

        // add content type based on extension
        addContentTypeHeaderForFormat(request, extension);

        // get spring specific attributes of request, e.g., path variables mapped to their values
        Map<String, String> springRequestAttributes =
                (Map<String, String>)
                        request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        // if a path variable ends in the "." + extension, remove this ending, because its is to
        // indicate content type
        //        for (Map.Entry<String, String> attributeMapping :
        // springRequestAttributes.entrySet()) {
        //            String value = attributeMapping.getValue();
        //            String dotExtension = "." + extension;
        //            if (value.endsWith(dotExtension)) {
        //                attributeMapping.setValue(value.substring(0, dotExtension.length()));
        //            }
        //        }
    }

    private static boolean mutateFormatRequest(MutableHttpServletRequest request) {
        boolean mutated = false;
        String format = request.getParameter(FORMAT);
        if ((request.getRequestURI().endsWith(DOWNLOAD) || request.getRequestURI().endsWith(SEARCH))
                && Utils.notNullNotEmpty(format)) {
            addContentTypeHeaderForFormat(request, format);
            mutated = true;
        }
        return mutated;
    }

    private static boolean mutateSearchOrDownloadRequest(MutableHttpServletRequest request) {
        boolean mutated = false;
        String format = request.getParameter(FORMAT);
        if ((request.getRequestURI().endsWith(DOWNLOAD) || request.getRequestURI().endsWith(SEARCH))
                && Utils.notNullNotEmpty(format)) {
            addContentTypeHeaderForFormat(request, format);
            mutated = true;
        }
        return mutated;
    }

    private static boolean mutateEntryRequest(MutableHttpServletRequest request) {
        boolean mutated = false;
        Matcher entryContextMatcher = ENTRY_CONTEXT_PATH_MATCHER.matcher(request.getRequestURL());
        if (entryContextMatcher.matches()) {
            String entryPathVariable = entryContextMatcher.group(2);
            String entryId = entryContextMatcher.group(3);
            String extension = entryContextMatcher.group(4);

            setRealEntryId(request, entryPathVariable, entryId);

            setURI(request, extension);
            setURL(request, extension);

            addContentTypeHeaderForFormat(request, extension);
            mutated = true;
        }
        return mutated;
    }

    private static void addDefaultAcceptHeaderIfRequired(
            boolean mutated, MutableHttpServletRequest request) {
        /*
        ensure bean that knows about the end-point's valid content types is able to look up the
        end-point's valid content types here, and store in X

        1. set content type requested
        1.1 take from accept header
        1.2 if request fits entry request pattern, extract from extension
        1.3 if request contains format parameter, extract from format parameter
        1.4 if no content type still, set to default content type, json

        2. if content type requested is NOT inside X
        2.1 if user-agent is a browser, then set content type to json
        2.2 otherwise
        2.2.1 if used extension/format to set content type (and this code block => it is not known), then
              throw HttpMediaTypeNotAcceptableException with unknown format message
        2.2.2 else (=> not derived from extension/format), throw HttpMediaTypeNotAcceptableException
              with message unknown content type specified
         */

        // if no accept header was added based on format/extension, then add default content type
        if (!mutated
                && (Utils.nullOrEmpty(request.getHeader(HttpHeaders.ACCEPT))
                        || (Utils.notNullNotEmpty(request.getHeader(HttpHeaders.ACCEPT))
                                && (request.getHeader(HttpHeaders.ACCEPT).equals("*/*")
                                        || !ALLOWED_ACCEPT_HEADERS.contains(
                                                request.getHeader(HttpHeaders.ACCEPT)))))) {
            request.addHeader(HttpHeaders.ACCEPT, DEFAULT_MEDIA_TYPE_VALUE);
        }
    }

    private static void addContentTypeHeaderForFormat(
            MutableHttpServletRequest request, String format) {
        try {
            MediaType mediaTypeForFileExtension =
                    UniProtMediaType.getMediaTypeForFileExtension(format);
            request.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());
        } catch (IllegalArgumentException iae) {
            request.addHeader(
                    HttpHeaders.ACCEPT, createUnknownMediaTypeForFileExtension(format).toString());
        }
    }

    private static void setURL(MutableHttpServletRequest request, String extension) {
        request.setRequestURL(
                request.getRequestURL()
                        .substring(0, request.getRequestURL().length() - (extension.length() + 1)));
    }

    private static void setURI(MutableHttpServletRequest request, String extension) {
        request.setRequestURI(
                request.getRequestURI()
                        .substring(0, request.getRequestURI().length() - (extension.length() + 1)));
    }

    private static void setRealEntryId(
            MutableHttpServletRequest request, String entryPathVariable, String entryId) {
        Map<String, String> uriVariablesMap = new HashMap<>();
        uriVariablesMap.put(entryPathVariable, entryId);

        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariablesMap);
    }
}
