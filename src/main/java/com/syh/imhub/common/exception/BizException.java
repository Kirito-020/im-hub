package com.syh.imhub.common.exception;

public class BizException extends RuntimeException {
    // 业务错误码：决定 HTTP 状态码与响应体 code，不允许为空
    private final ErrorCode errorCode;

    /** 只用错误码构造：提示文案取 ErrorCode 里的默认中文文案。参数 errorCode 为业务错误码。 */
    public BizException(ErrorCode errorCode) {
        // 把枚举里的默认文案作为异常 message，前端可直接展示
        super(errorCode.msg());
        // 保存错误码，供全局异常处理器映射状态码
        this.errorCode = errorCode;
    }

    /** 错误码 + 自定义文案构造：需要对用户说更具体的原因时用（例如带上限流阈值、工单号）。参数 errorCode 业务错误码、message 自定义提示。 */
    public BizException(ErrorCode errorCode, String message) {
        // 用调用方给的文案覆盖默认文案
        super(message);
        // 错误码照旧保存，状态码映射不受自定义文案影响
        this.errorCode = errorCode;
    }

    /** 取业务错误码；全局异常处理器与 WS 错误帧靠它决定返回码。 */
    public ErrorCode getErrorCode() {
        // 返回构造时保存的错误码
        return errorCode;
    }

    /** 静态工厂：用默认文案快速抛错，写法 BizException.of(ErrorCode.X)。参数 code 业务错误码；返回新异常实例。 */
    public static BizException of(ErrorCode code) {
        // 直接复用单参构造，文案取枚举默认值
        return new BizException(code);
    }

    /** 静态工厂：带自定义文案。参数 code 业务错误码、message 自定义提示；返回新异常实例。 */
    public static BizException of(ErrorCode code, String message) {
        // 交给双参构造，文案由调用方决定
        return new BizException(code, message);
    }
}
