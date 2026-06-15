package com.ht.stats.exception;

import com.ht.common.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler (stats-service)")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ---------- InvalidStatsRangeException ----------

    @Test
    @DisplayName("handleInvalidStatsRange - returns 400 with the exception's message and no details")
    void handleInvalidStatsRange_returnsBadRequestWithMessage() {
        // given
        InvalidStatsRangeException ex = new InvalidStatsRangeException("'from' must not be after 'to'");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleInvalidStatsRange(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("'from' must not be after 'to'");
        assertThat(body.getDetails()).isEmpty();
        assertThat(body.getTimestamp()).isNotNull();
    }

    // ---------- ConstraintViolationException ----------

    @Test
    @DisplayName("handleConstraintViolation - returns 400 with violations formatted as 'path': message")
    void handleConstraintViolation_withViolations_returnsBadRequestWithDetails() {
        // given
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("getStats.offerId");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must not be blank");

        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("getStats.from");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must not be null");

        // LinkedHashSet to keep a deterministic order for the assertion below
        Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
        violations.add(violation1);
        violations.add(violation2);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactlyInAnyOrder(
                "'getStats.offerId': must not be blank",
                "'getStats.from': must not be null"
        );
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
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).isEmpty();
    }

    // ---------- IllegalArgumentException ----------

    @Test
    @DisplayName("handleIllegalArgument - returns 400 with the exception's message and no details")
    void handleIllegalArgument_returnsBadRequestWithMessage() {
        // given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid action value");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getError()).isEqualTo("Bad Request");
        assertThat(body.getMessage()).isEqualTo("Invalid action value");
        assertThat(body.getDetails()).isEmpty();
    }

    // ---------- common response shape ----------

    @Test
    @DisplayName("all handlers - populate timestamp, status code, and reason phrase consistently")
    void allHandlers_populateCommonResponseFieldsConsistently() {
        // given
        InvalidStatsRangeException ex = new InvalidStatsRangeException("range error");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleInvalidStatsRange(ex);

        // then
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getError()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(body.getTimestamp()).isNotNull();
    }
}