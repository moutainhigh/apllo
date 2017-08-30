package com.gofobao.framework.financial.service;

import com.gofobao.framework.financial.entity.Eve;

import java.util.List;

/**
 * eve服务
 */
public interface EveService {
    List<Eve> findByRetseqnoAndSeqno(String retseqno, String seqno);

    Eve save(Eve eve);
}
