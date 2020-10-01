package org.uniprot.api.rest.validation.error;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.UNKNOWN_MEDIA_TYPE;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.ERROR_MESSAGE_ATTRIBUTE;
import static org.uniprot.api.rest.validation.error.ResponseExceptionHelper.*;
import static org.uniprot.core.util.Utils.nullOrEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.exception.NoContentException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.rest.request.MutableHttpServletRequest;
import org.uniprot.core.util.Utils;

/**
 * Captures exceptions raised by the application, and handles them in a tailored way.
 *
 * @author lgonzales
 */
@ControllerAdvice
public class ResponseExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ResponseExceptionHandler.class);
    private static final String NOT_FOUND_MESSAGE = "search.not.found";
    private static final String INTERNAL_ERROR_MESSAGE = "search.internal.error";
    private static final String REQUIRED_REQUEST_PARAM = "request.parameter.required";
    private static final String INVALID_REQUEST = "request.invalid";

    private MessageSource messageSource;

    public ResponseExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Bad Request exception handler that was caught during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<ErrorInfo> constraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();

        for (ConstraintViolation error : ex.getConstraintViolations()) {
            if (error.getMessage() != null) {
                messages.add(error.getMessage());
            }
        }

        addDebugError(request, ex, messages);

        return getBadRequestResponseEntity(request, messages);
    }

    /**
     * Resource not found exception handler that was caught during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 404 Not Found error response with error message details
     */
    @ExceptionHandler(value = {NoHandlerFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorInfo> noHandlerFoundException(
            Exception ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();
        messages.add(messageSource.getMessage(NOT_FOUND_MESSAGE, null, Locale.getDefault()));
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);
        addDebugError(request, ex, messages);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     * Internal Server Error exception handler
     *
     * @param ex throw exception
     * @param request http request
     * @return 500 Internal server error response
     */
    @ExceptionHandler({QueryRetrievalException.class, ServiceException.class, Throwable.class})
    public ResponseEntity<ErrorInfo> handleInternalServerError(
            Throwable ex, HttpServletRequest request) {
        String url = Encode.forHtml(request.getRequestURL().toString());
        String queryString = Encode.forHtml(request.getQueryString());
        String urlAndParams = queryString == null ? url : url + '?' + queryString;
        //NOSONAR
        logger.error("handleInternalServerError -- {}:", urlAndParams, ex);
        List<String> messages = new ArrayList<>();
        messages.add(messageSource.getMessage(INTERNAL_ERROR_MESSAGE, null, Locale.getDefault()));
        addDebugError(request, ex, messages);

        ErrorInfo error = new ErrorInfo(url, messages);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     * Bad Request exception handler that was caught during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler({BindException.class})
    public ResponseEntity<ErrorInfo> handleBindExceptionBadRequest(
            BindException ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();

        for (FieldError error : ex.getFieldErrors()) {
            String errorMessage = error.getDefaultMessage();
            if (Objects.nonNull(errorMessage)) {
                messages.add(errorMessage.replaceAll("\\{field\\}", error.getField()));
            }
        }

        for (ObjectError error : ex.getGlobalErrors()) {
            messages.add(error.getDefaultMessage());
        }

        addDebugError(request, ex, messages);

        return getBadRequestResponseEntity(request, messages);
    }

    /**
     * Bad Request exception handler that was caught during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorInfo> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        List<String> messages =
                singletonList(
                        messageSource.getMessage(
                                REQUIRED_REQUEST_PARAM,
                                new Object[] {ex.getParameterName()},
                                Locale.getDefault()));
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);

        addDebugError(request, ex, messages);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }

    /**
     * Bad Request exception handler that was caught during request
     *
     * @param ex throw exception
     * @param request http request
     * @return 400 Bad request error response with error message details
     */
    @ExceptionHandler({InvalidRequestException.class})
    public ResponseEntity<ErrorInfo> handleInvalidRequestExceptionBadRequest(
            InvalidRequestException ex, HttpServletRequest request) {
        List<String> messages =
                singletonList(
                        messageSource.getMessage(
                                INVALID_REQUEST,
                                new Object[] {ex.getMessage()},
                                Locale.getDefault()));

        addDebugError(request, ex, messages);

        return getBadRequestResponseEntity(request, messages);
    }

    /**
     * No content exception handler that was caught during processing of request. Note that a 204 no
     * content response must contain <i>no</i> body.
     *
     * @param ex thrown exception
     * @param request http request
     * @return 204 No content error response
     */
    @ExceptionHandler({NoContentException.class})
    public ResponseEntity<Void> handleNoContentExceptionNoContent(
            NoContentException ex, HttpServletRequest request) {
        addDebugError(request, ex, emptyList());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ExceptionHandler({HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<ErrorInfo> handleHttpMediaTypeNotAcceptableException(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request)
            throws HttpMediaTypeNotAcceptableException {
        if (Utils.notNull(request) && request instanceof MutableHttpServletRequest) {
            MutableHttpServletRequest mutableRequest = (MutableHttpServletRequest) request;
            MediaType contentType = getContentTypeFromRequest(request);
            if (contentType.equals(UNKNOWN_MEDIA_TYPE)) {
                mutableRequest.addHeader(HttpHeaders.ACCEPT, DEFAULT_MEDIA_TYPE_VALUE);
                String errorMessage = (String) mutableRequest.getAttribute(ERROR_MESSAGE_ATTRIBUTE);

                if (nullOrEmpty(errorMessage)) {
                    errorMessage = "Media type not acceptable";
                }
                List<String> messages =
                        singletonList(
                                messageSource.getMessage(
                                        INVALID_REQUEST,
                                        new Object[] {errorMessage},
                                        Locale.getDefault()));

                addDebugError(request, ex, messages);

                return getBadRequestResponseEntity(request, messages);
            }
        }
        throw ex;
    }
}
