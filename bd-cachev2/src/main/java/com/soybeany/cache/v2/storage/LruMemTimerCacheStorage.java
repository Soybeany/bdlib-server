package com.soybeany.cache.v2.storage;

import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.util.file.BdFileUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Soybeany
 * @date 2021/2/20
 */
public class LruMemTimerCacheStorage<Param, Data> extends LruMemCacheStorage<Param, Data> {

    private static ScheduledExecutorService SERVICE;

    private final Map<String, Task> taskMap = new HashMap<>();

    @SuppressWarnings("AlibabaThreadPoolCreation")
    public synchronized static void createTimer() {
        if (null == SERVICE) {
            SERVICE = Executors.newScheduledThreadPool(1);
        }
    }

    public synchronized static void destroyTimer() {
        if (null != SERVICE) {
            SERVICE.shutdown();
            SERVICE = null;
        }
    }

    @Override
    public String desc() {
        return super.desc() + "_TIMER";
    }

    @Override
    protected DataPack<Data> getCache(String key) throws NoCacheException {
        Task task = taskMap.get(key);
        if (null == task) {
            throw new NoCacheException();
        }
        task.uid = scheduleTask(key, task.delayInMillis);
        return super.getCache(key);
    }

    @Override
    protected synchronized void onCacheData(String key, CacheEntity<Data> cacheEntity) {
        long delayInMillis = cacheEntity.pExpireAt - System.currentTimeMillis();
        String uid = scheduleTask(key, delayInMillis);
        taskMap.put(key, new Task(uid, delayInMillis));
        super.onCacheData(key, cacheEntity);
    }

    // ********************内部方法********************

    private String scheduleTask(String key, long delayInMillis) {
        String uid = BdFileUtils.getUuid();
        SERVICE.schedule(() -> removeData(key, uid), delayInMillis, TimeUnit.MILLISECONDS);
        return uid;
    }

    private synchronized void removeData(String key, String uid) {
        Task task = taskMap.get(key);
        if (null == task || !uid.equals(task.uid)) {
            return;
        }
        taskMap.remove(key);
        super.removeCache(key);
    }

    // ********************内部类********************

    private static class Task {
        String uid;
        long delayInMillis;

        public Task(String uid, long delayInMillis) {
            this.uid = uid;
            this.delayInMillis = delayInMillis;
        }
    }

}
