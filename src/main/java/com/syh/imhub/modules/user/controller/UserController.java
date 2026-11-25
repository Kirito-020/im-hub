package com.syh.imhub.modules.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.syh.imhub.common.result.R;
import com.syh.imhub.infra.ws.PresenceService;
import com.syh.imhub.modules.user.entity.ImUser;
import com.syh.imhub.modules.user.mapper.ImUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequestMapping("/api/users")
@RestController
public class UserController {
    private final ImUserMapper UserMapper;
    private final PresenceService presence;

    public UserController(ImUserMapper userMapper, PresenceService presence) {
        this.UserMapper = userMapper;
        this.presence = presence;
    }


    public record UserBrief(Long id, String username, String nickname, String avatar, String role,
                            // online 由 PresenceService 实时计算；maxSessions 是并发接待上限
                            String skillGroup, boolean online, Integer maxSessions){}

    @GetMapping
    public R<List<UserBrief>> getUser(String keyword){
        //只取status=1的用户
        LambdaQueryWrapper<ImUser> wrapper = Wrappers.<ImUser>lambdaQuery().eq(ImUser::getStatus, 1);
        //判断keyword是否为空
        if (keyword != null && !keyword.isBlank()) {
            //模糊查询用户名
            wrapper.and(w->w
                    .like(ImUser::getUsername, keyword)
                    .or()
                    .like(ImUser::getNickname, keyword));
        }
        //只取前50条
        wrapper.last("limit 50");
        return R.ok(UserMapper.selectList(wrapper).stream().map(this::toBrief).toList());
    }

    private UserBrief toBrief(ImUser user) {
        return new UserBrief(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getRole(),
                user.getSkillGroup(),
                presence.isOnline(user.getId()),
                user.getMaxSessions()
        );
    }


}
