package com.ailee.retrofit.bean;

/**
 * Created by liwei on 2017/4/18 11:56
 * Email: liwei
 * Description: 分页参数
 */

public class PageResult implements Cloneable {
    private int curPage;
    private int pageSize;
    private boolean hasMore;//  是否有下一页
    private int total;//    总数
    private int maxSize;

    public int getCurPage() {
        return curPage;
    }

    public void setCurPage(int curPage) {
        this.curPage = curPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PageResult pageResult = null;
        try{
            pageResult = (PageResult) super.clone();
        }catch (CloneNotSupportedException e){
            e.printStackTrace();
        }
        return pageResult;
    }
}
