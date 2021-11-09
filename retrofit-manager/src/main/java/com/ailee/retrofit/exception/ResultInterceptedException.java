package com.ailee.retrofit.exception;


import com.ailee.retrofit.bean.BaseResp;

/**
 * Created by liwei on 2017/11/14 11:36
 * Email: liwei
 * Description: 表示接口返回被“劫持”的异常
 */
public class ResultInterceptedException extends Exception{
    private final BaseResp result;

    public ResultInterceptedException(BaseResp result) {
        this.result = result;
    }

    public BaseResp getResult() {
        return result;
    }
}
