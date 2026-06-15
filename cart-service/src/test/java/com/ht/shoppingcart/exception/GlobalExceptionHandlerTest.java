package com.ht.shoppingcart.exception;

import com.ht.common.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ---------- MethodArgumentNotValidException ----------

    @Test
    @DisplayName("handleValidation - returns 400 with field errors formatted as 'field': message")
    void handleValidation_withFieldErrors_returnsBadRequestWithDetails() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("cartItemDto", "offerId", "offerId is mandatory");
        FieldError fieldError2 = new FieldError("cartItemDto", "prices", "At least one price is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactly(
                "'offerId': offerId is mandatory",
                "'prices': At least one price is required"
        );
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleValidation - returns empty details list when there are no field errors")
    void handleValidation_noFieldErrors_returnsEmptyDetails() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetails()).isEmpty();
    }

    // ---------- ConstraintViolationException ----------

    @Test
    @DisplayName("handleConstraintViolation - returns 400 with violations formatted as 'path': message")
    void handleConstraintViolation_withViolations_returnsBadRequestWithDetails() {
        // given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("getCart.customerId");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactly("'getCart.customerId': must not be blank");
    }

    @Test
    @DisplayName("handleConstraintViolation - returns empty details when there are no violations")
    void handleConstraintViolation_noViolations_returnsEmptyDetails() {
        // given
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());

        // when
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetails()).isEmpty();
    }

    // ---------- HttpMessageNotReadableException ----------

    @Test
    @DisplayName("handleUnreadable - returns 400 with the most specific cause message prefixed")
    void handleUnreadable_returnsBadRequestWithCauseMessage() {
        // given
        Throwable rootCause = new IllegalArgumentException("Cannot deserialize value of type ActionType from String \"BUY\"");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(rootCause);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo(
                "Malformed request body: Cannot deserialize value of type ActionType from String \"BUY\"");
        assertThat(body.getDetails()).isEmpty();
    }

    // ---------- CartNotFoundException ----------

    @Test
    @DisplayName("handleCartNotFound - returns 404 with the exception's message")
    void handleCartNotFound_returnsNotFoundWithMessage() {
        // given
        CartNotFoundException ex = new CartNotFoundException("customer-1");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleCartNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
        assertThat(body.getMessage()).isEqualTo(ex.getMessage());
        assertThat(body.getDetails()).isEmpty();
    }

    // ---------- ItemNotFoundException ----------

    @Test
    @DisplayName("handleItemNotFound - returns 404 with the exception's message")
    void handleItemNotFound_returnsNotFoundWithMessage() {
        // given
        ItemNotFoundException ex = new ItemNotFoundException("customer-1", "offer-1");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleItemNotFound(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo(ex.getMessage());
        assertThat(body.getDetails()).isEmpty();
    }

    // ---------- General Exception ----------

    @Test
    @DisplayName("handleGeneral - returns 500 with a generic message and the exception's message in details")
    void handleGeneral_returnsInternalServerErrorWithExceptionMessageInDetails() {
        // given
        Exception ex = new RuntimeException("Unexpected database error");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getError()).isEqualTo("Internal Server Error");
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getDetails()).containsExactly("Unexpected database error");
    }

    // ---------- build() - common response shape ----------

    @Test
    @DisplayName("all handlers - populate timestamp, status code, and reason phrase consistently")
    void allHandlers_populateCommonResponseFieldsConsistently() {
        // given
        CartNotFoundException ex = new CartNotFoundException("customer-1");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleCartNotFound(ex);

        // then
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getError()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
        assertThat(body.getTimestamp()).isNotNull();
    }
}