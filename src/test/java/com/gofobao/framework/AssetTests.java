package com.gofobao.framework;

import com.gofobao.framework.asset.biz.AssetSynBiz;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.ExceptionEmailHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class AssetTests {

    final Gson GSON = new GsonBuilder().create();


    @Autowired
    AssetSynBiz assetSynBiz ;

    @Autowired
    ExceptionEmailHelper exceptionEmailHelper ;

    @Test
    public void test01() throws Exception {
        Date synDate = DateHelper.stringToDate("2017-09-12", DateHelper.DATE_FORMAT_YMD) ;
        assetSynBiz.doAdminSynAsset(16858L, synDate) ;
    }

    @Test
    public void test02() throws Exception {
        exceptionEmailHelper.sendErrorMessage("测试多人发送", "测试多人发送");
    }



    @Test
    public void test03() throws Exception{
        String url = "http://gofobao-admin.dev/504" ;
        try{
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.exchange(url, HttpMethod.POST, null, Map.class);
        }catch (Exception e){
            log.error("请求异常", e.getMessage());
            if(e instanceof HttpClientErrorException){
                if(e.getMessage().contains("405")){
                    log.error("405");
                }
            }else if(e instanceof HttpServerErrorException){
                if(e.getMessage().contains("502")){
                    log.error("502");
                }else if(e.getMessage().contains("504")){
                    log.error("504");
                }
            }
        }
    }
}
