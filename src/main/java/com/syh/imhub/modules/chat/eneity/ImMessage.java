package com.syh.imhub.modules.chat.eneity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("im_message")
public class ImMessage {

    // 主键策略：数据库自增，插入后回填到 id
    @TableId(type = IdType.AUTO)
    // 消息主键
    private Long id;
    // 所属会话
    private Long conversationId;
    // 会话内单调递增序号，前端排序与增量同步的游标
    private Long seq;
    // 发送者用户 ID，0 表示系统消息
    private Long senderId;
    // 客户端消息 ID，与 sender_id 组成幂等唯一键
    private String clientMsgId;
    // 消息类型：1 文本 / 2 图片 / 3 文件 / 4 系统
    private Integer type;
    // 正文，已做敏感词过滤；撤回后置空
    private String content;
    // 扩展信息 JSON 字符串（url、文件名等），空时为 null
    private String extra;
    // 状态：1 正常 / 2 已撤回
    private Integer status;
    // 落库时间，同时用于 2 分钟撤回窗口判断
    private LocalDateTime createdAt;
}
