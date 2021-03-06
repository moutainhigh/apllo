package com.gofobao.framework.aspect;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.CaptchaHelper;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.IpHelper;
import com.gofobao.framework.helper.RedisHelper;
import com.gofobao.framework.message.vo.request.VoAnonEmailReq;
import com.gofobao.framework.message.vo.request.VoAnonSmsReq;
import com.gofobao.framework.message.vo.request.VoUserSmsReq;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 短信拦截
 * Created by Max on 17/5/31.
 */
@Aspect
@Component
@Slf4j
public class MessageAspect {
    /** 一天最大操作次数 */
    private static final int OP_MAX_DAY = 5 ;

    /** ip最大访问次数 */
    private static final int IP_MAX_DAY = 24 ;

    /** 冻结次数*/
    private static final int FREEZE_TIME = 24 * 60 * 60 ;

    /** 操作间隙*/
    private static final int OP_INTERVAL_TIME = 60 ;

    @Autowired
    RedisHelper redisHelper ;

    @Value("${gofobao.captcha}")
    boolean captchaState;

    @Autowired
    CaptchaHelper captchaHelper ;

    @Pointcut("execution(public * com.gofobao.framework.message.controller..*.*(..))")
    public void cheat(){}


    @Around("cheat()")
    public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable{
        String name = String.format("%s.%s",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName()) ;

        // 获取参数
        Object[] args = joinPoint.getArgs();
        if(!ObjectUtils.isEmpty(args)){
            VoAnonSmsReq voAnonSmsReq = null ;
            VoUserSmsReq voUserSmsReq = null ;
            VoAnonEmailReq voAnonEmailReq = null ;
            Long userId = null ;
            String ip = null ;
            for(Object arg : args) {
                if (arg instanceof VoAnonSmsReq) {
                    voAnonSmsReq = (VoAnonSmsReq) arg;
                    if (ObjectUtils.isEmpty(voAnonSmsReq)) {
                        throw new Throwable("MessageAspect invoke: convert error ");
                    }

                } else if (arg instanceof HttpServletRequest) {
                    HttpServletRequest req = (HttpServletRequest) arg;
                    if (ObjectUtils.isEmpty(req)) {
                        throw new Throwable("MessageAspect invoke: convert error ");
                    }

                    ip = IpHelper.getIpAddress(req);
                } else if (arg instanceof VoUserSmsReq) {
                    voUserSmsReq = (VoUserSmsReq) arg;
                    if (ObjectUtils.isEmpty(voUserSmsReq)) {
                        throw new Throwable("MessageAspect invoke: convert error ");
                    }

                } else if (arg instanceof Long) {
                    userId = (Long) arg;
                    if (ObjectUtils.isEmpty(userId)) {
                        throw new Throwable("MessageAspect invoke: userId is empty ");
                    }

                } else if (arg instanceof VoAnonEmailReq) {
                    voAnonEmailReq = (VoAnonEmailReq) arg;
                    if (ObjectUtils.isEmpty(voAnonEmailReq)) {
                        throw new Throwable("MessageAspect invoke: convert error ");
                    }
                }
            }

            if( (!ObjectUtils.isEmpty(voUserSmsReq)) && (ObjectUtils.isEmpty(userId)) ) {
                throw new Throwable("MessageAspect invoke: convert error ") ;
            }

            if( (ObjectUtils.isEmpty(voAnonSmsReq)) && (ObjectUtils.isEmpty(voUserSmsReq)) && (ObjectUtils.isEmpty(voAnonEmailReq))){
                throw new Throwable("MessageAspect invoke: key is empty ") ;
            }

            //=========================================
            // 图形验证码
            //=========================================
            if (verifyCaptcha(voAnonSmsReq, voUserSmsReq, voAnonEmailReq))
                return ResponseEntity.
                    badRequest().
                    body(VoBaseResp.error(VoBaseResp.ERROR, "图形验证码错误或者已过期"));

            //=========================================
            // 操作间隔
            //=========================================
            if (verifyOpInterval(name, voAnonSmsReq, voUserSmsReq, voAnonEmailReq,  userId))
                return ResponseEntity.
                    badRequest().
                    body(VoBaseResp.error(VoBaseResp.ERROR, "操作间隙必须大于60S"));

            //=========================================
            // 操作次数限制功能
            //=========================================
            return verifyOpCount(joinPoint, name, voAnonSmsReq, voUserSmsReq, voAnonEmailReq, userId, ip);

        }else{
            throw new Throwable("MessageAspect invoke: joinPoint.getArgs() is empty") ;
        }
    }

    private boolean verifyOpInterval(String name, VoAnonSmsReq voAnonSmsReq, VoUserSmsReq voUserSmsReq, VoAnonEmailReq voAnonEmailReq, Long userId) throws Exception {
        String key = null ;
        key = getkey(voAnonSmsReq, voUserSmsReq, voAnonEmailReq, userId);
        String interval = redisHelper.get(String.format("%s_%s_interval", name.toUpperCase(), key), null);
        if(!ObjectUtils.isEmpty(interval)){
            return true;
        }else{
            redisHelper.put(String.format("%s_%s__interval", name.toUpperCase(), key), DateHelper.dateToString(new Date()), OP_INTERVAL_TIME);
        }
        return false;
    }

    private String getkey(VoAnonSmsReq voAnonSmsReq, VoUserSmsReq voUserSmsReq, VoAnonEmailReq voAnonEmailReq, Long userId) {
        String key;
        if(!ObjectUtils.isEmpty(voAnonEmailReq)){
            key = voAnonEmailReq.getEmail() ;
        }else if(!ObjectUtils.isEmpty(voUserSmsReq)){
            key = String.valueOf(userId);
        }else{
            key = voAnonSmsReq.getPhone() ;
        }
        return key;
    }

    /**
     * 操作次数判断
     * @param joinPoint
     * @param name
     * @param voAnonSmsReq
     * @param voUserSmsReq
     * @param voAnonEmailReq
     *@param userId
     * @param ip   @return
     * @throws Throwable
     */
    private Object verifyOpCount(ProceedingJoinPoint joinPoint,
                                 String name,
                                 VoAnonSmsReq voAnonSmsReq,
                                 VoUserSmsReq voUserSmsReq,
                                 VoAnonEmailReq voAnonEmailReq, Long userId,
                                 String ip) throws Throwable {
        String key = getkey(voAnonSmsReq, voUserSmsReq, voAnonEmailReq, userId);
        // 从缓存中获取操作次数
        Integer ipCount = Integer.parseInt(
                redisHelper.get(
                        String.format("%s_%s", name.toUpperCase(), ip),
                        "0"));

        if(ipCount >= IP_MAX_DAY) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前网路请求过于频繁，已经被系统拒绝！")) ;
        }

        Integer opCount =  Integer.parseInt(
                redisHelper.get(String.format("%s_%s", name.toUpperCase(), key),
                        "0"));

        if(opCount >= OP_MAX_DAY){
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前操作过于频繁，已经被系统拒绝！")) ;
        }

        Object proceed = joinPoint.proceed();
        if(proceed instanceof  ResponseEntity){
            redisHelper.put(
                    String.format("%s_%s", name.toUpperCase(), ip),
                    String.valueOf(++ipCount),
                    FREEZE_TIME);  // ip 请求增加

            ResponseEntity entity = (ResponseEntity) proceed;
            if(!entity.getStatusCode().equals(HttpStatus.OK)){
                redisHelper.put(
                        String.format("%s_%s", name.toUpperCase(), key),
                        String.valueOf(++opCount),
                        FREEZE_TIME); // 当前用户请求增加
            }

            return proceed ;
        }else{
            throw new Throwable("短信发送方法不符合规范：controller public 方法必须返回 ResponseEntity") ;
        }
    }

    /**
     * 验证图形验证码
     * @param voAnonSmsReq
     * @param voUserSmsReq
     * @param voAnonEmailReq
     * @return
     */
    private boolean verifyCaptcha(VoAnonSmsReq voAnonSmsReq, VoUserSmsReq voUserSmsReq, VoAnonEmailReq voAnonEmailReq) {
        if(captchaState){
            String captchaToken = null ;
            String captcha = null ;

            if(!ObjectUtils.isEmpty(voAnonSmsReq)){
                captchaToken = voAnonSmsReq.getCaptchaToken();
                captcha = voAnonSmsReq.getCaptcha();
            }else if(!ObjectUtils.isEmpty(voUserSmsReq)){
                captchaToken = voUserSmsReq.getCaptchaToken() ;
                captcha = voUserSmsReq.getCaptcha() ;
            }else{
                captchaToken = voAnonEmailReq.getCaptchaToken() ;
                captcha = voAnonEmailReq.getCaptcha() ;
            }

            Preconditions.checkNotNull(captchaToken, "captchaToken is empty");
            Preconditions.checkNotNull(captcha, "captcha is empty");
            if(!captchaHelper.match(captchaToken, captcha)){
                return true;
            }
        }
        return false;
    }


}
