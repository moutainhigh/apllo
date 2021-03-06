package com.gofobao.framework.comment.controller;

import com.gofobao.framework.comment.service.TopicService;
import com.gofobao.framework.comment.vo.request.VoTopicReq;
import com.gofobao.framework.comment.vo.response.VoTopicListResp;
import com.gofobao.framework.comment.vo.response.VoTopicResp;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.security.contants.SecurityContants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * Created by xin on 2017/11/8.
 */
@RestController
public class TopicController {

    @Autowired
    private TopicService topicService;

    @PostMapping("/comment/topic/publish")
    public ResponseEntity<VoBaseResp> publishTopic(HttpServletRequest httpServletRequest,
                                                   @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId,
                                                   @Valid @ModelAttribute VoTopicReq voTopicReq) {

        return topicService.publishTopic(voTopicReq, userId, httpServletRequest);
    }

    @GetMapping("/comment/topic/del/{id}")
    public ResponseEntity<VoBaseResp> delTopic(@PathVariable long id, @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        return topicService.delTopic(id, userId);
    }

    @GetMapping("/pub/comment/topic/list/{topicTypeId}/{page}")
    public ResponseEntity<VoTopicListResp> listTopic(HttpServletRequest httpServletRequest,
                                                     @PathVariable long topicTypeId,
                                                     @PathVariable(name = "page") Integer pageable,
                                                     HttpServletResponse response) {

        return topicService.listTopic(httpServletRequest, topicTypeId, pageable);
    }

    @GetMapping("/pub/comment/topic/findTopic/{topicId}")
    public ResponseEntity<VoTopicResp> findTopic(@PathVariable long topicId, HttpServletRequest httpServletRequest) {
        return topicService.findTopic(topicId, httpServletRequest);
    }


}


