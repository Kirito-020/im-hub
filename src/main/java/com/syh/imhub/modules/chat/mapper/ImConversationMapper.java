package com.syh.imhub.modules.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syh.imhub.modules.chat.eneity.ImConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImConversationMapper extends BaseMapper<ImConversation> {
    @Select("SELECT c.* FROM im_conversation c\n" +
            "            JOIN im_conversation_member m ON m.conversation_id = c.id\n" +
            "            WHERE m.user_id = #{userId} AND m.left_at IS NULL\n" +
            "            ORDER BY m.pinned DESC, c.last_msg_at DESC, c.id DESC")
    List<ImConversation> selectByUser(Long userId);

    @Select("SELECT * FROM im_conversation WHERE biz_key = #{bizKey}")
    ImConversation selectByBizKey(String bizKey);
}
