package com.temperature.proxy.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("WeatherProviderException")
class WeatherProviderExceptionTest {

    @Nested
    @DisplayName("Timeout Exception")
    class TimeoutExceptionTests {

        @Test
        void should_create_timeout_exception_with_message_and_cause() {
            var message = "Request timed out after 5 seconds";
            var cause = new TimeoutException("Connection timeout");

            var exception = WeatherProviderException.timeout(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
        }

        @Test
        void should_create_timeout_exception_with_different_cause_type() {
            var message = "API timeout";
            var cause = new IOException("Network timeout");

            var exception = WeatherProviderException.timeout(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
        }

        @Test
        void should_create_timeout_exception_with_null_cause() {
            var message = "Timeout occurred";

            var exception = WeatherProviderException.timeout(message, null);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
        }

        @Test
        void should_create_timeout_exception_with_empty_message() {
            var message = "";
            var cause = new TimeoutException();

            var exception = WeatherProviderException.timeout(message, cause);

            assertThat(exception.getMessage()).isEmpty();
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
        }
    }

    @Nested
    @DisplayName("Unavailable Exception")
    class UnavailableExceptionTests {

        @Test
        void should_create_unavailable_exception_with_message_and_cause() {
            var message = "Service temporarily unavailable";
            var cause = new IOException("Connection refused");

            var exception = WeatherProviderException.unavailable(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.UNAVAILABLE);
        }

        @Test
        void should_create_unavailable_exception_with_http_error() {
            var message = "HTTP 503 Service Unavailable";
            var cause = new RuntimeException("Service down");

            var exception = WeatherProviderException.unavailable(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.UNAVAILABLE);
        }

        @Test
        void should_create_unavailable_exception_with_null_cause() {
            var message = "Service unavailable";

            var exception = WeatherProviderException.unavailable(message, null);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.UNAVAILABLE);
        }

        @Test
        void should_create_unavailable_exception_with_network_error() {
            var message = "Network error";
            var cause = new IOException("No route to host");

            var exception = WeatherProviderException.unavailable(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("Invalid Response Exception")
    class InvalidResponseExceptionTests {

        @Test
        void should_create_invalid_response_exception_with_message() {
            var message = "Response body is empty";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
        }

        @Test
        void should_create_invalid_response_exception_for_missing_field() {
            var message = "Required field 'temperature' is missing";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
        }

        @Test
        void should_create_invalid_response_exception_for_malformed_json() {
            var message = "Failed to parse JSON response";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
        }

        @Test
        void should_create_invalid_response_exception_with_empty_message() {
            var message = "";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getMessage()).isEmpty();
            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
        }

        @Test
        void should_create_invalid_response_exception_without_cause() {
            var message = "Invalid data format";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Type")
    class ErrorTypeTests {

        @ParameterizedTest
        @EnumSource(WeatherProviderException.ErrorType.class)
        void should_have_all_error_types_defined(WeatherProviderException.ErrorType errorType) {
            assertThat(errorType).isNotNull();
            assertThat(errorType.name()).isNotEmpty();
        }

        @Test
        void should_have_timeout_error_type() {
            var exception = WeatherProviderException.timeout("timeout", null);

            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
        }

        @Test
        void should_have_unavailable_error_type() {
            var exception = WeatherProviderException.unavailable("unavailable", null);

            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.UNAVAILABLE);
        }

        @Test
        void should_have_invalid_response_error_type() {
            var exception = WeatherProviderException.invalidResponse("invalid");

            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
        }

        @Test
        void should_have_exactly_three_error_types() {
            var errorTypes = WeatherProviderException.ErrorType.values();

            assertThat(errorTypes).hasSize(4);
            assertThat(errorTypes)
                    .containsExactlyInAnyOrder(
                            WeatherProviderException.ErrorType.TIMEOUT,
                            WeatherProviderException.ErrorType.UNAVAILABLE,
                            WeatherProviderException.ErrorType.INVALID_RESPONSE,
                            WeatherProviderException.ErrorType.UPSTREAM_ERROR);
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy")
    class ExceptionHierarchyTests {

        @Test
        void should_be_runtime_exception() {
            var exception = WeatherProviderException.timeout("test", null);

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        void should_be_throwable() {
            var exception = WeatherProviderException.unavailable("test", null);

            assertThat(exception).isInstanceOf(Throwable.class);
        }

        @Test
        void should_preserve_cause_chain() {
            var rootCause = new IOException("Root cause");
            var intermediateCause = new RuntimeException("Intermediate", rootCause);
            var exception = WeatherProviderException.timeout("Final message", intermediateCause);

            assertThat(exception.getCause()).isEqualTo(intermediateCause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
        }

        @Test
        void should_have_stack_trace() {
            var exception = WeatherProviderException.invalidResponse("test");

            assertThat(exception.getStackTrace()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        void should_create_different_instances_for_each_factory_call() {
            var message = "test";
            var cause = new IOException();

            var exception1 = WeatherProviderException.timeout(message, cause);
            var exception2 = WeatherProviderException.timeout(message, cause);

            assertThat(exception1).isNotSameAs(exception2);
        }

        @Test
        void should_support_method_chaining_pattern() {
            var exception = WeatherProviderException.timeout("Timeout occurred", new TimeoutException());

            assertThat(exception.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
            assertThat(exception.getMessage()).isEqualTo("Timeout occurred");
        }

        @Test
        void should_handle_long_messages() {
            var longMessage = "A".repeat(1000);

            var exception = WeatherProviderException.invalidResponse(longMessage);

            assertThat(exception.getMessage()).hasSize(1000);
        }

        @Test
        void should_handle_special_characters_in_message() {
            var message = "Error: \n\t\"invalid\" <data> & 'chars'";

            var exception = WeatherProviderException.invalidResponse(message);

            assertThat(exception.getMessage()).isEqualTo(message);
        }
    }
}
