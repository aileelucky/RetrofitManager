package com.ailee.retrofit.bean;

/**
 * Created by liwei on 2017/4/18 11:58
 * Email: liwei
 * Description: 通用的分页列表实体类
 */

public class SimplePagedListResp<T> extends BaseResp {
    private PagedBean<T> data;

    public PagedBean<T> getData() {
        return data;
    }

    public void setData(PagedBean<T> data) {
        this.data = data;
    }
}
