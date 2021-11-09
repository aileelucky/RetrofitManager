package com.ailee.retrofit;


import com.ailee.retrofit.bean.BaseResp;

import retrofit2.Call;

/**
 * Created by liwei on 2017/4/18 10:51
 * Email: liwei
 * Description: AdvancedRetrofitCallback 的简单实现，需要哪那个回调复写哪个
 */

public class SimpleRetrofitCallback<T> implements AdvancedRetrofitCallback<T> {

    @Override
    public void onSuccess(Call<T> call, T resp) {
        // do nothing
    }

    @Override
    public void onError(Call<T> call, String errorCode, String msg) {
        // do nothing
    }

    @Override
    public void onError(Call<T> call, BaseResp baseResp) {
        // do nothing
    }

    @Override
    public void onIntercepted(Call<T> call, BaseResp resp) {
        // do nothing
    }

    @Override
    public void onEnd(Call<T> call) {
        // do nothing
    }

    @Override
    public void onCanceled() {
        // do nothing
    }
}
