package smartfloor.controller.advice;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import smartfloor.domain.exception.CannotUnarchiveUserException;
import smartfloor.domain.exception.CompositeUserNotFoundException;
import smartfloor.domain.exception.FallRiskProfileNotFoundException;
import smartfloor.domain.exception.FloorInaccessibleException;
import smartfloor.domain.exception.FloorNotFoundException;
import smartfloor.domain.exception.FootstepNotFoundException;
import smartfloor.domain.exception.GroupAlreadyExistsException;
import smartfloor.domain.exception.GroupLimitExceededTenantLimitException;
import smartfloor.domain.exception.GroupNotFoundException;
import smartfloor.domain.exception.GroupUserLimitReachedException;
import smartfloor.domain.exception.InterventionNotFoundException;
import smartfloor.domain.exception.InvalidInterventionsException;
import smartfloor.domain.exception.InvalidMeasurementTypeException;
import smartfloor.domain.exception.TenantNotFoundException;
import smartfloor.domain.exception.TenantUserLimitReachedException;
import smartfloor.domain.exception.TestResultNotFoundException;
import smartfloor.domain.exception.UserCountExceededNewLimitException;
import smartfloor.domain.exception.UserIsArchivedException;
import smartfloor.domain.exception.UserMeasurementNotFoundException;
import smartfloor.domain.exception.UserNotFoundException;
import smartfloor.domain.exception.UserNotLinkedException;
import smartfloor.domain.exception.UserWearableLinkNotFoundException;
import smartfloor.domain.exception.WearableAlreadyExistsException;
import smartfloor.domain.exception.WearableInUseException;
import smartfloor.domain.exception.WearableNotFoundException;
import smartfloor.domain.exception.WearableNotInUseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            {
                    IllegalArgumentException.class, InvalidMeasurementTypeException.class,
                    InvalidInterventionsException.class
            }
    )
    public final ResponseEntity<Object> handleBadRequestException(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(
            {
                    FallRiskProfileNotFoundException.class,
                    FloorNotFoundException.class, FootstepNotFoundException.class,
                    GroupNotFoundException.class, TenantNotFoundException.class, UserNotFoundException.class,
                    UserWearableLinkNotFoundException.class, WearableNotFoundException.class,
                    UserMeasurementNotFoundException.class, TestResultNotFoundException.class,
                    CompositeUserNotFoundException.class, InterventionNotFoundException.class
            }
    )
    public ResponseEntity<Object> handleNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(
            {
                    WearableInUseException.class, WearableNotInUseException.class, UserNotLinkedException.class,
                    GroupAlreadyExistsException.class, WearableAlreadyExistsException.class
            }
    )
    public ResponseEntity<Object> handleUsageException(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        return new ResponseEntity<>("Access Denied: " + ex.getMessage(), HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(
            {
                    GroupLimitExceededTenantLimitException.class,
                    UserCountExceededNewLimitException.class,
                    CannotUnarchiveUserException.class,
                    UserIsArchivedException.class,
                    GroupUserLimitReachedException.class,
                    TenantUserLimitReachedException.class,
                    FloorInaccessibleException.class
            }
    )
    public ResponseEntity<Object> handleForbiddenException(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    /**
     * Handle any validation errors.
     *
     * @param e the exception that Spring throws when validation fails
     * @return a response with the validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        List<String> errors = e.getBindingResult().getAllErrors().stream()
                .map(error -> error.getCode() + " : " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", String.join("\n", errors)));
    }
}
