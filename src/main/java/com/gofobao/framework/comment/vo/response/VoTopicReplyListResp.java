package com.gofobao.framework.comment.vo.response;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * Created by xin on 2017/11/13.
 */
@Data
@ApiModel
public class VoTopicReplyListResp extends VoBaseResp {
    private List<VoTopicReplyResp> voTopicReplyResps = Lists.newArrayList();
}
