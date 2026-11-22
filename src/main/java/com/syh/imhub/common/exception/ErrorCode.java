package com.syh.imhub.common.exception;

public enum ErrorCode {
    // 成功：所有正常响应都用它，前端以 code == 0 判断成功
    OK(0, "成功"),
    // 参数校验失败：DTO 校验不通过、缺参、类型不匹配、上传超限都用它
    PARAM_INVALID(40001, "参数校验失败"),
    // 未登录或令牌过期：AuthInterceptor 与 UserContext.require() 抛出
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    // 无权访问：越权操作他人会话/工单、非管理员调用管理接口
    FORBIDDEN(40301, "无权访问该资源"),
    // 资源不存在：查不到的会话、工单、用户，以及未知的静态资源路径
    NOT_FOUND(40401, "资源不存在"),
    // 数据冲突：乐观锁失败、重复提交、坐席接待数已达上限
    CONFLICT(40901, "数据冲突，请重试"),
    // 操作过于频繁：目前用于发消息的 10 秒窗口限流
    RATE_LIMITED(42901, "操作过于频繁"),
    // 消息包含敏感内容：敏感词命中且动作级别为「拦截」时抛出
    SENSITIVE_BLOCKED(46001, "消息包含敏感内容"),
    // 会话已被其他坐席接入：客服会话 CAS 抢单失败时抛出
    SESSION_TAKEN(47001, "该会话已被其他坐席接入"),
    // 系统异常兜底：未预期异常统一返回它，前端只展示通用提示
    SYS_ERROR(50000, "系统繁忙，请稍后再试");

    // 业务码：响应体里返回给前端的数字判断依据，与 HTTP 状态码分离
    private final int code;
    // 默认提示文案：BizException 没有传自定义文案时用它
    private final String msg;

    /** 枚举构造：每个错误码绑定一个数字 code 和一句默认中文提示。参数 code 业务码、msg 默认提示文案。 */
    ErrorCode(int code, String msg) {
        // 存业务码数字
        this.code = code;
        // 存默认中文提示
        this.msg = msg;
    }

    /** 取业务码数字：用于响应体 code、WS ERROR 帧和状态码映射的 switch。 */
    public int code() {
        // 返回数字码
        return code;
    }

    /** 取默认中文提示：BizException 单参构造和拦截器直接写响应体时使用。 */
    public String msg() {
        // 返回默认提示文案
        return msg;
    }
}
