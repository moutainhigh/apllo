package com.gofobao.framework.system.biz.impl;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.service.UserService;
import com.gofobao.framework.system.biz.NoticesBiz;
import com.gofobao.framework.system.entity.Notices;
import com.gofobao.framework.system.service.NoticesService;
import com.gofobao.framework.system.vo.request.VoNoticesReq;
import com.gofobao.framework.system.vo.request.VoNoticesTranReq;
import com.gofobao.framework.system.vo.response.UnReadMsgNumWarpRes;
import com.gofobao.framework.system.vo.response.UserNotices;
import com.gofobao.framework.system.vo.response.VoViewNoticesInfoWarpRes;
import com.gofobao.framework.system.vo.response.VoViewUserNoticesWarpRes;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by Max on 17/6/5.
 */
@Component
@Slf4j
public class NoticesBizImpl implements NoticesBiz {

    @Autowired
    NoticesService noticesService;

    @Autowired
    UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Notices notices) {
        Preconditions.checkNotNull(notices, "NoticesBizImpl.save: notices is empty");
        Preconditions.checkNotNull(notices.getUserId(), "NoticesBizImpl.save: userId is empty");

        Users users = userService.findByIdLock(notices.getUserId());
        if (ObjectUtils.isEmpty(users)) {
            log.error("NoticesBizImpl.save: userId find null");
            return false;
        }

        Date now = new Date();
        if (ObjectUtils.isEmpty(notices.getCreatedAt())) {
            notices.setCreatedAt(now);
        }

        if (ObjectUtils.isEmpty(notices.getUpdatedAt())) {
            notices.setUpdatedAt(now);
        }

        noticesService.save(notices);
        Integer noticeCount = users.getNoticeCount();
        noticeCount = noticeCount <= 0 ? 0 : noticeCount;
        users.setNoticeCount(noticeCount + 1);
        users.setUpdatedAt(now);
        userService.save(users);
        return true;
    }

    /**
     * 站内信列表
     *
     * @param voNoticesReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewUserNoticesWarpRes> list(VoNoticesReq voNoticesReq) {
        try {
            VoViewUserNoticesWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewUserNoticesWarpRes.class);
            List<UserNotices> userNotices = noticesService.list(voNoticesReq);
            warpRes.setTotalCount(0);
            if (!CollectionUtils.isEmpty(userNotices)) {
                warpRes.setTotalCount(userNotices.get(0).getTotalCount());
                userNotices.get(0).setTotalCount(null);
            }
            warpRes.setNotices(userNotices);
            return ResponseEntity.ok(warpRes);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(
                            VoBaseResp.error(
                                    VoBaseResp.ERROR,
                                    "查询失败",
                                    VoViewUserNoticesWarpRes.class));
        }
    }

    /**
     * 站内信内容
     *
     * @param voNoticesReq
     * @return
     */
    @Transactional
    @Override
    public ResponseEntity<VoViewNoticesInfoWarpRes> info(VoNoticesReq voNoticesReq) {
        try {
            VoViewNoticesInfoWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewNoticesInfoWarpRes.class);
            VoNoticesTranReq voNoticesTranReq = new VoNoticesTranReq();

            voNoticesTranReq.setUserId(voNoticesReq.getUserId());
            voNoticesTranReq.setNoticesIds(Lists.newArrayList(voNoticesReq.getId()));
            Boolean falg = noticesService.update(voNoticesTranReq);
            if (!falg) {

                return ResponseEntity.badRequest()
                        .body(
                                VoBaseResp.error(
                                        VoBaseResp.ERROR,
                                        "查询失败",
                                        VoViewNoticesInfoWarpRes.class));
            }
            warpRes.setNoticesInfo(noticesService.info(voNoticesReq));
            return ResponseEntity.ok(warpRes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(
                            VoBaseResp.error(
                                    VoBaseResp.ERROR,
                                    "查询失败",
                                    VoViewNoticesInfoWarpRes.class));
        }

    }


    /**
     * 批量删除
     *
     * @param voNoticesTranReq
     * @return
     */
    @Override
    public ResponseEntity<VoBaseResp> delete(VoNoticesTranReq voNoticesTranReq) {
        try {
            boolean result = noticesService.delete(voNoticesTranReq);
            if (result)
                return ResponseEntity.ok(VoBaseResp.ok("删除成功"));
            else
                return ResponseEntity.badRequest()
                        .body(VoBaseResp.error(
                                VoBaseResp.ERROR,
                                "删除失败"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "删除失败"));
        }
    }

    /**
     * 更新成功
     *
     * @param voNoticesTranReq
     * @return
     */
    @Override
    public ResponseEntity<VoBaseResp> update(VoNoticesTranReq voNoticesTranReq) {
        try {
            boolean result = noticesService.update(voNoticesTranReq);
            if (result)
                return ResponseEntity.ok(VoBaseResp.ok("更新成功"));
            else
                return ResponseEntity.badRequest()
                        .body(VoBaseResp.error(
                                VoBaseResp.ERROR,
                                "更新失败"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "更新失败"));
        }

    }

    @Override
    public ResponseEntity<UnReadMsgNumWarpRes> unRead(Long userId) {
        try {
            UnReadMsgNumWarpRes warpRes = VoBaseResp.ok("查询成功", UnReadMsgNumWarpRes.class);
            warpRes.setNum(noticesService.unread(userId));
            return ResponseEntity.ok(warpRes);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(
                            VoBaseResp.error(
                                    VoBaseResp.ERROR,
                                    "查询失败",
                                    UnReadMsgNumWarpRes.class));
        }

    }
}
