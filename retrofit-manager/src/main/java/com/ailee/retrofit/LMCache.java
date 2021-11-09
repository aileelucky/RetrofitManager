package com.ailee.retrofit;

import android.content.Context;
import android.text.TextUtils;


import com.ailee.retrofit.cahe.SMInternalCache;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.Request;
import okhttp3.internal.http.HttpMethod;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

/**
 * Created by liwei on 2018/3/17 16:56
 * Email: liwei
 * Description: 缓存相关操作
 */
public class LMCache {

    private static final String ROOT_DIR_NAME = "api_cache";

    // /data/data/{包名}/cache/api_cache
    private File rootDir;
    // /data/data/{包名}/cache/api_cache/{version}
    private File versionDir;
    // /data/data/{包名}/cache/api_cache/{version}/{id}
    private File cacheDir;
    // 保存需要从接口刷新数据的缓存的 key
    private Set<String> needRefreshSet = Collections.synchronizedSet(new HashSet<String>());

    public LMCache(Context context, String version, String id) {
        rootDir = new File(context.getCacheDir(), ROOT_DIR_NAME);

        setVersion(version);
        setId(id);
    }

    final SMInternalCache internalCache = new SMInternalCache() {
        @Override
        public void put(Request request, String value) {
            LMCache.this.put(request, value);
        }

        @Override
        public String get(Request request) {
            return LMCache.this.get(request);
        }

        @Override
        public BufferedSource getBufferedSource(Request request) throws IOException {
            return LMCache.this.getBufferedSource(request);
        }

        @Override
        public boolean remove(Request request) {
            return LMCache.this.remove(request);
        }

        @Override
        public boolean isValid(Request request, long cacheTime) {
            return LMCache.this.isValid(request, cacheTime);
        }

        @Override
        public boolean needRefresh(Request request) {
            return LMCache.this.needRefresh(request);
        }

        @Override
        public void setNeedRefresh(Request request) {
            LMCache.this.setNeedRefresh(request);
        }

        @Override
        public void removeNeedRefresh(Request request) {
            LMCache.this.removeNeedRefresh(request);
        }

        @Override
        public boolean clear() {
            return LMCache.this.clear();
        }
    };

    private void setVersion(String version) {
        if (TextUtils.isEmpty(version)) {
            throw new IllegalArgumentException("LMCache version cant not be empty!");
        }

        // 将版本号进行 MD5 加密
        String encodedVersion = md5Hex(version);

        // 更新 version 目录
        versionDir = new File(rootDir, encodedVersion);
        // 删除其他版本的缓存目录，为了防止卡顿采用异步的方式
        deleteOtherVersionDirsAsync(encodedVersion);
    }

    /**
     * 异步删除其他版本的缓存目录
     *
     * @param currentVersion 当前的版本号
     */
    private void deleteOtherVersionDirsAsync(final String currentVersion) {
        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                deleteOtherVersionDirs(currentVersion);
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 删除其他版本的缓存目录
     *
     * @param currentVersion 当前的版本号
     */
    private void deleteOtherVersionDirs(String currentVersion) {
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // 当前版本目录
                if (file.isDirectory() && currentVersion.equals(file.getName())) continue;
                deleteDir(file);
            }
        }
    }

    /**
     * 根据 id 设置当前缓存目录
     * 如果 id 为空，缓存目录为版本号目录，否则为版本号目录下的子目录
     *
     * @param id 用户标识
     */
    void setId(String id) {
        // 判断当前的缓存目录是否是版本号目录
        String lastId = null;
        if (cacheDir != null
                && cacheDir.exists()
                && cacheDir.getParentFile().equals(versionDir)) {
            lastId = cacheDir.getName();
        }

        if (!TextUtils.isEmpty(id)) {
            // 对 id 进行 MD5 加密
            String realId = md5Hex(id);
            // 新 id 不为空
            if (!realId.equals(lastId)) {
                // 新老 id 不相同，清除之前的缓存
                deleteOtherIdDirsAsync(realId);

                // 设置对应新 id 的缓存目录
                cacheDir = new File(versionDir, realId);
            }
        } else {
            // 新 id 为空
            if (!TextUtils.isEmpty(lastId)) {
                // 老 id 不为空，清除之前的缓存
                deleteOtherIdDirsAsync(id);
            }
            // 将当前缓存目录设置为版本号目录
            cacheDir = versionDir;
        }

        if (!cacheDir.exists()) cacheDir.mkdirs();
    }

    private String md5Hex(String s) {
        if (TextUtils.isEmpty(s)) return s;
        return ByteString.encodeUtf8(s).md5().hex();
    }

    /**
     * 异步删除版本目录下的文件和其他 id 缓存的目录
     *
     * @param currentId 当前 id
     */
    private void deleteOtherIdDirsAsync(final String currentId) {
        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                deleteOtherIdDirs(currentId);
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 删除版本目录下的文件和其他 id 缓存的目录
     *
     * @param currentId 当前 id
     */
    private void deleteOtherIdDirs(String currentId) {
        File[] files = versionDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(currentId)) continue;
                deleteDir(file);
            }
        }
    }

    /**
     * 判断缓存目录是否有效
     * 目录存在或者目录不存在但能创建成功表示有效，否则表示无效
     */
    private boolean isCacheDirInvalid() {
        return cacheDir == null || !cacheDir.exists() && !cacheDir.mkdirs();
    }

    /**
     * 新增或更新对应请求缓存
     */
    void put(Request request, String value) {
        String requestMethod = request.method();
        // 下面的 if 代码块是从 okhttp3.Cache 中拿过来的
        // 意思是如果这次请求是 POST/PUT 等修改数据的操作
        // 那对应的缓存一定是失效了，需要删除
        if (HttpMethod.invalidatesCache(requestMethod)) {
            remove(request);
        }

        if (!"GET".equals(requestMethod)) {
            // 不缓存非 GET 请求的返回数据
            return;
        }

        // 移除对应请求缓存需要刷新的标志
        removeNeedRefresh(request);
        File cacheFile = getCacheFileFor(request);
        if (cacheFile == null) return;

        BufferedSink bufferedSink = null;
        try {
            bufferedSink = Okio.buffer(Okio.sink(cacheFile));
            bufferedSink.writeUtf8(value);
        } catch (IOException ignored) {
        } finally {
            if (bufferedSink != null) {
                try {
                    bufferedSink.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 移除对应请求的缓存
     */
    boolean remove(Request request) {
        // 移除对应请求需要刷新的标志
        removeNeedRefresh(request);
        File cacheFile = getCacheFileFor(request);
        return cacheFile != null && cacheFile.exists() && cacheFile.delete();
    }

    /**
     * 获取对应请求的缓存
     */
    String get(Request request) {
        File cacheFile = getCacheFileFor(request);
        if (cacheFile != null && cacheFile.exists()) {
            BufferedSource fileBuffer = null;
            try {
                fileBuffer = Okio.buffer(Okio.source(cacheFile));
                return fileBuffer.readUtf8();
            } catch (IOException ignored) {
            } finally {
                if (fileBuffer != null) {
                    try {
                        fileBuffer.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取对应请求缓存的 BufferedSource 对象
     */
    BufferedSource getBufferedSource(Request request) throws FileNotFoundException {
        File cachedFile = getCacheFileFor(request);
        if (cachedFile != null && cachedFile.exists()) {
            return Okio.buffer(Okio.source(cachedFile));
        }
        return null;
    }

    /**
     * 根据缓存时间判断对应请求的缓存是否有效
     */
    boolean isValid(Request request, long cacheTime) {
        File cacheFile = getCacheFileFor(request);
        return cacheFile != null
                && cacheFile.exists()
                && cacheFile.lastModified() + cacheTime * 1000 > System.currentTimeMillis();
    }

    /**
     * 查询请求对象对应的缓存数据是否需要从接口刷新
     *
     * @param request okhttp 请求对象
     * @return 需要返回 true，否则返回 false
     */
    boolean needRefresh(Request request) {
        String cacheKey = cacheKey(request);
        return needRefreshSet.contains(cacheKey);
    }

    /**
     * 将请求对象对应的缓存数据设置为需要从接口刷新
     *
     * @param request okhttp 请求对象
     */
    void setNeedRefresh(Request request) {
        needRefreshSet.add(cacheKey(request));
    }

    /**
     * 移除请求对象对应缓存数据的需要从接口刷新标志
     *
     * @param request okhttp 请求对象
     */
    void removeNeedRefresh(Request request) {
        needRefreshSet.remove(cacheKey(request));
    }

    /**
     * 获取对应请求的缓存文件对象
     */
    private File getCacheFileFor(Request request) {
        if (request == null) return null;
        if (isCacheDirInvalid()) return null;

        return new File(cacheDir, cacheKey(request) + ".lmc");
    }

    /**
     * 获取对应请求的缓存文件名称
     *
     * @param request 请求对象
     */
    private String cacheKey(Request request) {
        return Cache.key(request.url());
    }

    /**
     * 清除缓存
     */
    boolean clear() {
        // 清除所有的缓存数据刷新标志
        needRefreshSet.clear();
        return !isCacheDirInvalid() && deleteDir(cacheDir);
    }

    private boolean deleteDir(File dir) {
        if (dir == null) return false;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDir(file);
                }
            }
        }
        return dir.delete();
    }
}
