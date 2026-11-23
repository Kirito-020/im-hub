package com.syh.imhub.common.exception;

import com.syh.imhub.common.result.R;
import com.syh.imhub.common.util.TraceId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // 只用于兜底异常和排查类日志；正常业务失败走 BizException 分支不会打 error 日志
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> onBiz(BizException e) {
        // 先取出业务错误码，下面的状态码映射和响应体 code 都用它
        ErrorCode code = e.getErrorCode();
        // 业务码 → HTTP 状态码，让前端既能按 status 也能按 code 判断
        HttpStatus status = switch (code) {
            // 未登录/令牌失效 → 401，前端清掉本地 token 跳登录页
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            // 无权访问 → 403
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            // 资源不存在 → 404
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            // 数据冲突、客服会话已被抢 → 409，前端提示重试或刷新列表
            case CONFLICT, SESSION_TAKEN -> HttpStatus.CONFLICT;
            // 触发限流 → 429
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            // 敏感词被拦截 → 422，前端提示用户修改文案
            case SENSITIVE_BLOCKED -> HttpStatus.UNPROCESSABLE_ENTITY;
            // 参数校验失败 → 400
            case PARAM_INVALID -> HttpStatus.BAD_REQUEST;
            // 其余（含系统繁忙、SYS_ERROR）→ 500
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        // 按映射出的状态码返回，响应体仍是统一 R 结构
        return ResponseEntity.status(status).body(R.fail(code.code(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> onValid(MethodArgumentNotValidException e) {
        // 逐个取字段错误里的默认提示（来自 DTO 注解上的 message），用分号拼成一条可读文案
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        // 统一返回 400，业务码用 PARAM_INVALID
        return ResponseEntity.badRequest().body(R.fail(ErrorCode.PARAM_INVALID.code(), msg));
    }

    /**
     * 处理请求参数缺失、类型不匹配以及代码里直接抛的 IllegalArgumentException：都归为参数错误。
     * 参数 e：具体异常（缺参 / 类型转换失败 / 非法入参）；返回 400 + PARAM_INVALID，msg 取异常自带信息。
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<R<Void>> onBadRequest(Exception e) {
        // 缺参、类型不匹配、非法入参都属于「调用方传参不对」：返回 400 并把异常原文回显给调用方，方便联调定位
        return ResponseEntity.badRequest().body(R.fail(ErrorCode.PARAM_INVALID.code(), e.getMessage()));
    }

    /**
     * 处理上传文件超过 multipart 上限的异常，避免落到兜底分支变成 500。
     * 参数 e：Spring 的上传超限异常；返回 413 + PARAM_INVALID，提示固定为 10MB（与 application.yml 的上传配置保持一致）。
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> onUploadTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        // 返回 413 并给出明确的体积提示，前端据此提示用户压缩图片后重传
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(R.fail(ErrorCode.PARAM_INVALID.code(), "文件不能超过 10MB"));
    }

    /**
     * 处理未知路径/静态资源 404：Spring Boot 3 对没匹配上的静态资源会抛 NoResourceFoundException。
     * 参数 e：静态资源未找到异常；返回 404 + NOT_FOUND，避免被兜底分支转成 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> onNotFound(NoResourceFoundException e) {
        // 明确回 404，前端路由兜底页据此展示「页面不存在」
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail(ErrorCode.NOT_FOUND.code(), "资源不存在"));
    }

    /**
     * 兜底处理所有未预期异常：打带堆栈的 error 日志并返回 500，响应体带 traceId 方便对日志。
     * 参数 e：任意未捕获异常；request 用于日志里记录出问题的 URI；
     * 返回 500 + SYS_ERROR，且不把异常原文回显给前端。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> onOther(Exception e, HttpServletRequest request) {
        // 取本次请求的 traceId，前后端排查时用它去日志里捞完整堆栈
        String traceId = TraceId.get();
        // 记录 URI + traceId + 异常堆栈，这是线上定位 500 的主要入口
        log.error("unhandled error traceId={} uri={}", traceId, request.getRequestURI(), e);
        // 构造统一失败响应：code=SYS_ERROR，msg 用枚举里的通用文案，再附上 traceId
        R<Void> body = new R<>(ErrorCode.SYS_ERROR.code(), ErrorCode.SYS_ERROR.msg(), null, traceId);
        // 返回 500；具体异常原因只留在服务端日志，不返回给前端
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
