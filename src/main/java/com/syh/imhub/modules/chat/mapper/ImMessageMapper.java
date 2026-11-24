package com.syh.imhub.modules.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syh.imhub.modules.chat.eneity.ImMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessage> {
    @Select("select * from im_message where conversation_id = #{id} order by seq desc limit 1")
    ImMessage selectByLast(Long id);

    @Select("select count(*) from im_message " +
            "where conversation_id = #{conversationId} and seq > #{readSeq} and sender_id <> #{userId} and status = 1")
    long countUnread(Long userId, Long conversationId, long readSeq);
}
