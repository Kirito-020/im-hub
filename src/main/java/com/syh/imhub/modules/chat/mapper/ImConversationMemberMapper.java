package com.syh.imhub.modules.chat.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syh.imhub.modules.chat.eneity.ImConversationMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ImConversationMemberMapper extends BaseMapper<ImConversationMember> {
    @Select("SELECT * FROM im_conversation_member " +
            "WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    ImConversationMember selectByConversationIdAndUserId(Long conversationId, Long userId);

    @Update("UPDATE im_conversation_member SET joined_at = #{joinedAt}, " +
            "left_at = #{leftAt}, member_role = #{memberRole} " +
            "WHERE id = #{id}")
    void updateById(Long id, LocalDateTime joinedAt, LocalDateTime leftAt, int memberRole);
}
