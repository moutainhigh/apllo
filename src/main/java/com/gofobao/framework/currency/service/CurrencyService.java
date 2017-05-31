package com.gofobao.framework.currency.service;

import com.gofobao.framework.currency.entity.Currency;

/**
 * Created by Zeke on 2017/5/23.
 */
public interface CurrencyService {

    Currency findByUserId(Long userId);

    Currency findByUserIdLock(Long userId);

    boolean insert(Currency currency);

    boolean updateById(Currency currency);
}
