package com.syh.imhub.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("im_user")
public class ImUser {
    // 主键策略：交给数据库自增，插入后回填到 id
    @TableId(type = IdType.AUTO)
    // 用户主键，同时是 JWT 的 sub 与消息的 sender_id
    private Long id;
    // 登录名，唯一索引；注册与登录都会先 trim + 转小写
    private String username;
    // BCrypt 密码哈希（含盐），任何情况下都不存明文
    private String password;
    // 昵称：会话标题、消息发送者名都用它
    private String nickname;
    // 头像地址，注册时为空串
    private String avatar;
    // 角色：USER / AGENT / SUPERVISOR / ADMIN，决定权限码
    private String role;
    // 技能组，工单按此分组路由到坐席队列
    private String skillGroup;
    // 并发接待上限：坐席 5，普通用户 1
    private Integer maxSessions;
    // 账号状态：1 启用、0 禁用
    private Integer status;
    // 创建时间，由数据库默认值写入
    private LocalDateTime createdAt;
    // 最后更新时间，由数据库 ON UPDATE 维护
    private LocalDateTime updatedAt;
    // 逻辑删除注解：查询自动追加 deleted = 0
    @TableLogic
    // 删除标记：0 未删、1 已删
    private Integer deleted;
}
