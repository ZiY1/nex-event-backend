package io.github.ziy1.nexevent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage<T> {
    @JsonProperty("status_code")
    private Integer code;

    @JsonProperty("status_message")
    private String message;

    @JsonProperty("payload")
    private T data;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("path")
    private String path;

    // Static factory methods
    public static ResponseMessage<Void> success(String path) {
        return new ResponseMessage<>(HttpStatus.OK.value(), "success", null, LocalDateTime.now(), path);
    }
    public static <T> ResponseMessage<T> success(String path, T data) {
        return new ResponseMessage<>(HttpStatus.OK.value(), "success", data, LocalDateTime.now(), path);
    }

    public static <T> ResponseMessage<T> success(String message, String path, T data) {
        return new ResponseMessage<>(HttpStatus.OK.value(), message, data, LocalDateTime.now(), path);
    }

    public static <T> ResponseMessage<T> error(String path, String message) {
        return new ResponseMessage<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null, LocalDateTime.now(), path);
    }

    public static <T> ResponseMessage<T> error(HttpStatus status, String path, String message, T data) {
        return new ResponseMessage<>(status.value(), message, data, LocalDateTime.now(), path);
    }

    public static <T> ResponseMessage<T> noContent(String path, String message) {
        return new ResponseMessage<>(HttpStatus.NO_CONTENT.value(), message, null, LocalDateTime.now(), path);
    }
}