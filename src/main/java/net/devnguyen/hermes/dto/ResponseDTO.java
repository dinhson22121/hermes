package net.devnguyen.hermes.dto;

import lombok.Data;

@Data
public class ResponseDTO<T> {

    public static ResponseDTO<Void> success() {
        return new ResponseDTO<>(true, null, null, null);
    }

    public static <K> ResponseDTO<K> success(K data) {
        return new ResponseDTO<K>(true, data, null, null);
    }

    public static ResponseDTO<?> fail(String message) {
        return new ResponseDTO<>(false, null, message, null);
    }

    public static <K> ResponseDTO<K> fail(String message, K data) {
        return new ResponseDTO<K>(false, data, message, null);
    }

    public static <K> ResponseDTO<K> fail(String message, Class<K> kClass) {
        return new ResponseDTO<>(false, null, message, null);
    }

    public static <K> ResponseDTO<K> fail(Exception exception, Class<K> kClass) {
        return new ResponseDTO<>(false, null, exception.getMessage(), exception);
    }

    public static <K> ResponseDTO<K> fail(Exception exception, K data) {
        return new ResponseDTO<>(false, data, exception.getMessage(), exception);
    }

    public boolean isNotOk() {
        return !isOk();
    }

    private boolean ok;
    private T data;
    private String message;
    private Exception exception;

    public ResponseDTO(boolean ok, T data, String message, Exception exception) {
        this.ok = ok;
        this.data = data;
        this.message = message;
        this.exception = exception;
    }

    private String _toString;

    public String toString() {
        if (_toString != null) {
            return _toString;
        }
        _toString = ok ? "SUCCESS" : "FAILED";
        if (message != null) {
            _toString += "|" + message;
        }
        if (message == null && exception != null) {
            _toString += "|" + exception.getMessage();
        }
//        if (data != null) {
//            _toString += "|" + Utils.toJson(data);
//        }

        return _toString;
    }
}
