package com.gofobao.framework.comment.controller;

import com.gofobao.framework.comment.service.TopicCommentService;
import com.gofobao.framework.comment.vo.request.VoTopicCommentReq;
import com.gofobao.framework.comment.vo.response.VoTopicCommentListResp;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.RedisHelper;
import com.gofobao.framework.security.contants.SecurityContants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * Created by xin on 2017/11/10.
 */
@RestController
public class TopicCommentController {
    @Autowired
    private TopicCommentService topicCommentService;

    @GetMapping("/pub/comment/topic-comment/list/{topicId}/{pageIndex}")
    public ResponseEntity<VoTopicCommentListResp> listComment(HttpServletRequest httpServletRequest,
                                                              @PathVariable Long topicId,
                                                              @PathVariable Integer pageIndex,
                                                              @RequestParam(value = "hot", defaultValue = "false") Boolean hot) {
        if (ObjectUtils.isEmpty(pageIndex) || pageIndex <= 0) {
            pageIndex = 1;
        }
        return topicCommentService.listDetail(httpServletRequest, topicId, pageIndex, hot);
    }
    @PostMapping("/comment/topic/comment/publish")
    public ResponseEntity<VoBaseResp> publishComment(@Valid @ModelAttribute VoTopicCommentReq voTopicCommentReq,
                                                     @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        return topicCommentService.publishComment(voTopicCommentReq, userId);
    }

    @GetMapping("/comment/topic/comment/delete/{topicCommentId}")
    public ResponseEntity<VoBaseResp> delComment(@PathVariable Long topicCommentId,
                                                 @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        return topicCommentService.delComment(topicCommentId, userId);
    }


}
