package top.mryan2005.sspubot.sspubotbackend.Response;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * <h2 style="font-size: '11px'">统一响应信息类</h2>
 * <p style="font-size: '11px'">用于封装响应的状态码、消息和数据</p>
 * @author Mryan2005
 * @since Version 0.0.1
 * @param <T>
 */
@Data
public class ResponseMessage<T> {
    private Integer code;
    private String message;
    private T data;

    public ResponseMessage(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 200 成功 only for GET
     * @param data
     * @return 响应信息
     * @param <T>
     */
    public static <T> ResponseMessage success200(T data) {
        return new ResponseMessage(HttpStatus.OK.value(), "success!", data);
    }

    public static <T> ResponseMessage success() {
        return new ResponseMessage(HttpStatus.OK.value(), "success!", null);
    }

    /**
     * 201 成功 only for POST, PUT, DELETE
     * @param data
     * @return 响应信息
     * @param <T>
     */
    public static <T> ResponseMessage success201(T data) {
        return new ResponseMessage(HttpStatus.CREATED.value(), "success!", data);
    }

    /**
     * 500 服务器错误
     * @param data
     * @return 响应信息
     * @param <T>
     */
    public static <T> ResponseMessage error500(T data) {
        return new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "error!", data);
    }

    /**
     * 400 请求错误
     * @param data
     * @return 响应信息
     * @param <T>
     */
    public static <T> ResponseMessage error400(T data) {
        return new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "error!", data);
    }

    /**
     * 401 未授权
     * @param data
     * @return 响应信息
     * @param <T>
     */
    public static <T> ResponseMessage error401(T data) {
        return new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "error!", data);
    }

    public static <T> ResponseMessage error404(T data) {
        return new ResponseMessage(HttpStatus.NOT_FOUND.value(), "error!", data);
    }

    public static <T> ResponseMessage error9394(T data) {
        return new ResponseMessage(9394, "I am a brat", data);
    }

    public static <T> ResponseMessage error405(T data) {
        return new ResponseMessage(HttpStatus.METHOD_NOT_ALLOWED.value(), "error!", data);
    }

    public static <T> ResponseMessage error403(T data) {
        return new ResponseMessage(HttpStatus.FORBIDDEN.value(), "error!", data);
    }
}
