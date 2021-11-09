package com.ailee.retrofit.cahe;

import com.ailee.retrofit.bean.BaseResp;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Created by liwei on 2018/3/16 10:06
 * Email: liwei
 * Description: 缓存处理逻辑拦截器
 */
public class SMCacheInterceptor implements Interceptor {

    private static final MediaType jsonType = MediaType.parse("application/json;charset=UTF-8");

    public static final String HEADER_NEED_REFRESH = "Need-Refresh";
    public static final String HEADER_FROM_CACHE = "From-Cache";
    private static final String QUERY_CUR_PAGE = "curPage";

    private final SMInternalCache cache;
    private final JsonAdapter<BaseResp> baseRespJsonAdapter;

    public SMCacheInterceptor(SMInternalCache cache) {
        this.cache = cache;
        this.baseRespJsonAdapter = new Moshi.Builder().build().adapter(BaseResp.class);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        // 从缓存获取数据
        BufferedSource cacheBufferedSource = cache != null
                ? cache.getBufferedSource(request)
                : null;

        // 判断是否有缓存
        if (cacheBufferedSource != null) {
            // 有缓存
            // 判断是否是需要直接从接口获取数据的请求
            if (cache.needRefresh(request)) {
                cache.removeNeedRefresh(request);
                return proceed(chain);
            }

            // 构造缓存数据的 Response 对象
            ResponseBody body = ResponseBody.create(jsonType, -1,
                    cache.getBufferedSource(request));
            Response.Builder responseBuilder = new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body);

            // 将缓存数据返回
            return responseBuilder
                    .header(HEADER_FROM_CACHE, Boolean.toString(true))
                    .build();
        }

        // 没有缓存，从接口请求数据
        return proceed(chain);
    }

    /**
     * 从接口请求数据，并根据 cacheTime 和 status 字段判断是否要缓存返回的数据
     */
    private Response proceed(Chain chain) throws IOException {
        Request request = chain.request();
        // 从接口获取最新数据
        Response networkResponse = chain.proceed(request);

        // 如果接口请求不成功，直接返回
        if (!networkResponse.isSuccessful()) return networkResponse;

        ResponseBody body = networkResponse.body();
        if (body == null) return networkResponse;

        String bodyStr = body.string();
        BaseResp baseResp = parseBaseResp(bodyStr);

        // 因为 ResponseBody 已经被读取过了，所以需要重新构造
        return networkResponse.newBuilder()
                .body(ResponseBody.create(jsonType, bodyStr))
                .build();
    }

    /**
     * 将返回数据转换为 BaseResp
     *
     * @param body 字符串格式的返回数据
     */
    private BaseResp parseBaseResp(String body) throws IOException {
        if (body == null) return null;
        return baseRespJsonAdapter.fromJson(body);
    }
}
