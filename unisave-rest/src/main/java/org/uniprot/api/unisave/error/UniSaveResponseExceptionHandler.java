package org.uniprot.api.unisave.error;

import static org.uniprot.api.rest.validation.error.ResponseExceptionHelper.addDebugError;
import static org.uniprot.api.rest.validation.error.ResponseExceptionHelper.getContentTypeFromRequest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.uniprot.api.rest.validation.error.ErrorInfo;

/**
 * UniSave specific response error handling.
 *
 * <p>Created 20/04/2020
 *
 * @author Edd
 */
@ControllerAdvice
public class UniSaveResponseExceptionHandler {
    @ExceptionHandler(value = {UniSaveEntryNotFoundException.class})
    public ResponseEntity<ErrorInfo> noHandlerFoundException(
            Exception ex, HttpServletRequest request) {
        List<String> messages = new ArrayList<>();
        messages.add(ex.getMessage());
        ErrorInfo error = new ErrorInfo(request.getRequestURL().toString(), messages);
        addDebugError(request, ex, messages);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(getContentTypeFromRequest(request))
                .body(error);
    }
}
