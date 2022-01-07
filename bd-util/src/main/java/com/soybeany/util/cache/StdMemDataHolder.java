package com.soybeany.util.cache;

import com.soybeany.util.file.BdFileUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class StdMemDataHolder<T> implements IDataHolder<T> {

    private final Map<String, Task<T>> dataMap;
    private final boolean needAutoUpdate;

    private ScheduledExecutorService service;

    public StdMemDataHolder(int maxCount) {
        this(maxCount, true);
    }

    public StdMemDataHolder(int maxCount, boolean needAutoUpdate) {
        this.dataMap = new LruMap<>(maxCount);
        this.needAutoUpdate = needAutoUpdate;
    }

    @Override
    public synchronized T put(String key, T data, int expiryInSec) {
        String uid = scheduleTask(key, expiryInSec);
        Task<T> previous = dataMap.put(key, new Task<>(uid, data, expiryInSec));
        return getDataFromTask(previous);
    }

    @Override
    public synchronized T get(String key) {
        Task<T> task = dataMap.get(key);
        if (null == task) {
            return null;
        }
        if (needAutoUpdate) {
            task.uid = scheduleTask(key, task.expiryInSec);
        }
        return task.data;
    }

    @Override
    public synchronized T remove(String key) {
        Task<T> previous = dataMap.remove(key);
        return getDataFromTask(previous);
    }

    @Override
    public Collection<T> getAll() {
        return dataMap.values()
                .stream()
                .map(task -> task.data)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void clear() {
        dataMap.clear();
    }

    // ********************内部方法********************

    private T getDataFromTask(Task<T> task) {
        return Optional.ofNullable(task).map(t -> t.data).orElse(null);
    }

    @SuppressWarnings("AlibabaThreadPoolCreation")
    private String scheduleTask(String key, int expiryInSec) {
        String uid = BdFileUtils.getUuid();
        if (null == service) {
            service = Executors.newScheduledThreadPool(1);
        }
        service.schedule(() -> removeData(key, uid), expiryInSec, TimeUnit.SECONDS);
        return uid;
    }

    private synchronized void removeData(String key, String uid) {
        Task<T> task = dataMap.get(key);
        if (null == task || !uid.equals(task.uid)) {
            return;
        }
        dataMap.remove(key);
        if (dataMap.isEmpty() && null != service) {
            service.shutdownNow();
            service = null;
        }
    }

    // ********************内部类********************

    private static class LruMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LruMap(int capacity) {
            super(0, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    private static class Task<T> {
        String uid;
        T data;
        int expiryInSec;

        public Task(String uid, T data, int expiryInSec) {
            this.uid = uid;
            this.data = data;
            this.expiryInSec = expiryInSec;
        }
    }

}
