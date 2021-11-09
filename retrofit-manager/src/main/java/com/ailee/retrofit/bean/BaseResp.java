package com.ailee.retrofit.bean;

/**
 * Created by liwei on 2017/4/18 11:49
 * Email: liwei
 * Description: 实体类基类
 */

public class BaseResp {
    // 状态码
    private String status;
    // 提示信息
    private String msg;
    // 消息码
    private String code;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
