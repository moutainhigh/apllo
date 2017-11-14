package com.gofobao.framework.comment.repository;

import com.gofobao.framework.comment.entity.TopicReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by xin on 2017/11/13.
 */
@Repository
public interface TopicReplyRepository extends JpaRepository<TopicReply, Long>, JpaSpecificationExecutor<TopicReply> {
    List<TopicReply> findByTopicCommentId(Long topicCommentId);
}
