package com.syh.imhub.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.syh.imhub.common.exception.BizException;
import com.syh.imhub.common.exception.ErrorCode;
import com.syh.imhub.modules.auth.dto.LoginReq;
import com.syh.imhub.modules.auth.dto.LoginResp;
import com.syh.imhub.modules.auth.dto.RegisterReq;
import com.syh.imhub.modules.user.entity.ImUser;
import com.syh.imhub.modules.user.mapper.ImUserMapper;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Service
public class AuthService {

    private final JwtService jwtService;
    private final ImUserMapper imUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final StringRedisTemplate redis;
    private static final int MAX_LOGIN_FAILS = 10;
    private static final Duration LOGIN_LOCK = Duration.ofMinutes(5);

    public AuthService(JwtService jwtService, ImUserMapper imUserMapper, StringRedisTemplate redis) {
        this.jwtService = jwtService;
        this.imUserMapper = imUserMapper;
        this.redis = redis;
    }

    public LoginResp register(RegisterReq req) {
        //统一将用户名转为小写且去除首尾空格
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        //查询用户是否已存在
        Long exists = imUserMapper.selectCount(Wrappers.<ImUser>lambdaQuery().eq(ImUser::getUsername, username));
        //如果用户已存在则返回错误
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "用户已存在");
        }
        //创建用户
        ImUser user = new ImUser();
        user.setUsername(username);
        //存BCrypt哈希密码
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? username : req.nickname().trim());
        user.setAvatar("");
        //注册永远是用户角色
        String role = "USER";
        user.setRole(role);
        user.setSkillGroup(req.skillGroup() == null || req.skillGroup().isBlank() ? "default" : req.skillGroup());
        user.setMaxSessions("AGENT".equals(role) ? 5 : 1);
        user.setStatus(1);
        //保存用户
        imUserMapper.insert(user);
        //注册即登录，直接签发 JWT，前端不必再调用登录接口
        return issue(user);
    }

    public LoginResp login(LoginReq req) {
        //统一将用户名转为小写且去除首尾空格
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        //失败计数
        String failKey = "auth:login:fail:" + username;
        //读取失败次数
        String fails = redis.opsForValue().get(failKey);
        //如果失败次数达到一定次数则返回错误
        if (fails != null && Integer.parseInt(fails) >= MAX_LOGIN_FAILS) {
            throw BizException.of(ErrorCode.RATE_LIMITED, "登录失败次数过多，请5分钟再试");
        }
        ImUser user = imUserMapper.selectOne(Wrappers.<ImUser>lambdaQuery().eq(ImUser::getUsername, username));
        //用户名或密码错误合并成一个分支，避免泄露用户是否存在
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            //记录失败次数
            Long count = redis.opsForValue().increment(failKey);
            //只在第一次失败时设置过期时间（防止redis被大量无效失败请求撑爆）
            if (count != null && count == 1) {
                redis.expire(failKey, LOGIN_LOCK);
            }
            //抛异常
            throw BizException.of(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        //判断用户状态是否正常
        if(user.getStatus() != null && user.getStatus() == 0){
            //抛异常
            throw BizException.of(ErrorCode.FORBIDDEN, "用户状态异常");
        }
        //登陆成功清零失败次数
        redis.delete(failKey);
        return issue(user);
    }


    private LoginResp issue(ImUser user) {
        //签发 JWT
        JwtService.TokenPair pair = jwtService.issue(user.getId(), user.getRole());
        //返回登录响应
        return new LoginResp(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), toInfo(user));

    }

    public LoginResp.UserInfo toInfo(ImUser user) {
        return new LoginResp.UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                user.getRole(), user.getSkillGroup(), user.getMaxSessions(), PermissionOf(user.getRole()));
    }

    private static List<String> PermissionOf(String role) {
        // 所有角色都具备的基础能力：聊天、创建工单、查看工单
        List<String> list = new ArrayList<>(List.of("chat", "ticket:create", "ticket:view"));
        // role 为 null 时按 USER 处理，避免历史数据缺角色导致空指针
        switch (role == null ? "USER" : role) {
            // 坐席：坐席工作台 + 处理工单 + 快捷回复
            case "AGENT" -> {
                // 允许进入坐席工作台
                list.add("workbench");
                // 允许接手并处理工单
                list.add("ticket:handle");
                // 允许使用快捷回复语料
                list.add("quick-reply");
            }
            // 主管在坐席能力之上增加监控与派单
            case "SUPERVISOR" -> {
                // 主管同样需要工作台
                list.add("workbench");
                // 主管同样能处理工单
                list.add("ticket:handle");
                // 主管同样能用快捷回复
                list.add("quick-reply");
                // 增加实时监控看板
                list.add("monitor");
                // 增加把工单指派给坐席的权限
                list.add("ticket:assign");
            }
            // 管理员：在主管基础上再增加后台管理入口
            case "ADMIN" -> {
                // 管理员同样需要工作台
                list.add("workbench");
                // 管理员同样能看实时监控
                list.add("monitor");
                // 管理员同样能处理工单
                list.add("ticket:handle");
                // 管理员同样能派单
                list.add("ticket:assign");
                // 后台管理权限，可进入系统配置等页面
                list.add("admin");
                // 管理员同样能用快捷回复
                list.add("quick-reply");
            }
            // 未登记的角色（如 USER）只保留基础能力，不做额外追加
            default -> {
            }
        }
        // 返回权限码列表
        return list;

    }


    public LoginResp.UserInfo me(Long userId) {
        //根据用户ID查询用户信息
        ImUser user = imUserMapper.selectById(userId);
        if(user == null){
            throw BizException.of(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toInfo(user);
    }
}
