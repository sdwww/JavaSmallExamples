package com.demo.common.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：把所有异常归一化为 Result.fail，HTTP 永远 200（方案 A）。
 * 监控告警依赖此处的 warn / error 日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务可预期异常：warn 级别即可，不需要堆栈 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> onBiz(BusinessException e) {
        log.warn("Business error: code={}, msg={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    /** 参数校验类异常（MineSweeperGame 构造 / validateCoordinates 抛的） */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> onIllegalArg(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return Result.fail(ErrorCode.INVALID_COORDINATES.getCode(), e.getMessage());
    }

    /** Spring 请求参数类型转换失败（如 enum 值非法、int 传字符串） */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> onParamTypeMismatch(MethodArgumentTypeMismatchException e) {
        String msg = String.format("参数 %s 值非法: %s", e.getName(), e.getValue());
        log.warn("Param type mismatch: {}", msg);
        return Result.fail(ErrorCode.INVALID_PARAMETER.getCode(), msg);
    }

    /** Spring 缺少必传参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> onMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少参数: " + e.getParameterName();
        log.warn("Missing param: {}", msg);
        return Result.fail(ErrorCode.INVALID_PARAMETER.getCode(), msg);
    }

    /** 兜底：未知异常一定要 error + 堆栈，便于排查 */
    @ExceptionHandler(Exception.class)
    public Result<Void> onAny(Exception e) {
        log.error("Unexpected error", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
