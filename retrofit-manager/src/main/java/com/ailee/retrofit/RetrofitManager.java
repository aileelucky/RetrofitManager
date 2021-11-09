package com.ailee.retrofit;

import android.util.Log;


import com.ailee.retrofit.bean.BaseResp;
import com.ailee.retrofit.cahe.SMCacheInterceptor;
import com.ailee.retrofit.converter.SMGsonConverterFactory;
import com.ailee.retrofit.rx.adapter.LMRxJava2CallAdapterFactory;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Created by liwei on 2017/4/12 10:56
 * Email: liwei
 * Description: retrofit 配置管理类
 */

public class RetrofitManager {

    private static final String TAG = "RetrofitManager";

    static final int DEFAULT_CONNECT_TIMEOUT = 30;
    static final int DEFAULT_READ_TIMEOUT = 30;
    static final int DEFAULT_WRITE_TIMEOUT = 30;

    static ApiStatusInterceptor apiStatusInterceptor;

    private static Retrofit retrofit;
    private static OkHttpClient client;
    private static LMCache cache;

    public static void init(RetrofitProvider provider) {
        if (retrofit != null) {
            Log.w(TAG, "RetrofitManager already initialized!");
            return;
        }
        if (provider == null) {
            throw new IllegalArgumentException("RetrofitProvider must NOT be null!");
        }
        apiStatusInterceptor = provider.provideApiStatusInterceptor();
        cache = provider.provideCache();
        client = buildClient(provider.provideOkHttpConfig());
        retrofit = new Retrofit.Builder()
                .baseUrl(provider.provideBaseUrl())
                .addConverterFactory(SMGsonConverterFactory.create(BaseResp.class))
                .addCallAdapterFactory(LMRxJava2CallAdapterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(client)
                .build();
    }

    /**
     * 获取 OKHttpClient 对象，方便其他地方复用
     *
     * @return RetrofitManager 使用的 OkHttpClient 对象，如果 RetrofitManager 还未初始化，则为 null
     */
    public static OkHttpClient getOkHttpClient() {
        return client;
    }

    /**
     * 创建配置好的 OkHttpClient
     */
    private static OkHttpClient buildClient(OkHttpConfiguration config) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if (config != null) {
            List<Interceptor> interceptors = config.interceptors();
            if (interceptors != null) {
                clientBuilder.interceptors().addAll(interceptors);
            }
            // 连接超时
            int connectTimeoutSeconds = config.connectTimeoutSeconds();
            clientBuilder.connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS);
            // 读取超时
            int readTimeoutSeconds = config.readTimeoutSeconds();
            clientBuilder.readTimeout(readTimeoutSeconds, TimeUnit.SECONDS);
            // 写入超时
            int writeTimeoutSeconds = config.writeTimeoutSeconds();
            clientBuilder.writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS);

            X509TrustManager trustManager = config.trustManager();
            if (trustManager != null) {
                // 配置 https 证书
                SSLSocketFactory sslSocketFactory;
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, new TrustManager[]{trustManager}, null);
                    sslSocketFactory = sslContext.getSocketFactory();
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
                clientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
            }
        } else {
            // 设置默认超时时间
            // 连接超时
            clientBuilder.connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS);
            // 读取超时
            clientBuilder.readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS);
            // 写入超时
            clientBuilder.writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS);
        }
        if (cache != null) {
            // 增加缓存处理拦截器
            clientBuilder.addInterceptor(new SMCacheInterceptor(cache.internalCache));
        }
        return clientBuilder.build();
    }

    /**
     * 根据指定的 Service.Class ，得到一个被代理后的 Service
     */
    public static <T> T createService(Class<T> service) {
        if (retrofit == null) {
            throw new IllegalStateException("RetrofitManager not initialized! Call RetrofitManager.init() in your custom application class!");
        }
        return retrofit.create(service);
    }

    /**
     * 同步清除缓存
     */
    public static boolean clearCache() {
        if (cache != null) {
            return cache.clear();
        }
        return false;
    }

    /**
     * 异步清除缓存回调接口
     */
    public interface ClearCacheCallback {
        void onComplete(boolean result);
        void onError(Throwable t);
    }

    /**
     * 异步清除缓存
     * @param callback
     */
    public static void clearCacheAsync(final ClearCacheCallback callback) {
        Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return clearCache();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean result) throws Exception {
                        if (callback != null) {
                            callback.onComplete(result);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (callback != null) {
                            callback.onError(throwable);
                        }
                    }
                });
    }

    /**
     * 设置缓存 id ，这个 id 被用作缓存的子目录
     *
     * @param id 缓存 id，典型地，应该是当前登录用户 id
     */
    public static void setCacheId(String id) {
        if (cache != null) {
            cache.setId(id);
        }
    }
}
