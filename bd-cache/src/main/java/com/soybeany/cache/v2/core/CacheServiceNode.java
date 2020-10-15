package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.exception.*;
import com.soybeany.cache.v2.module.DataPack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
class CacheServiceNode<Param, Data> {

    private final Map<String, String> mKeyMap = new WeakHashMap<String, String>();

    private final ICacheService<Param, Data> mCurService;
    private final IKeyConverter<Param> mConverter;
    private final IDatasource<Param, Data> mDatasource;

    private CacheServiceNode<Param, Data> mNextNode;

    CacheServiceNode(ICacheService<Param, Data> curService, IKeyConverter<Param> converter, IDatasource<Param, Data> datasource) {
        if (null == curService) {
            throw new RuntimeException("CacheService不能为null");
        }
        if (null == converter) {
            throw new RuntimeException("KeyConverter不能为null");
        }
        mCurService = curService;
        mConverter = converter;
        mDatasource = datasource;
    }

    ICacheService<Param, Data> getService() {
        return mCurService;
    }

    /**
     * 设置下一个节点
     *
     * @param node 下一个节点
     */
    void setNextNode(CacheServiceNode<Param, Data> node) {
        mNextNode = node;
    }

    /**
     * 获取数据
     */
    DataPack<Data> getData(final String dataGroup, Param param, final DataAccessConfig config) throws DataException {
        return getDataFromCurService(dataGroup, param, mConverter.getKey(param), new IOnNoDataListener<Param, Data>() {
            @Override
            public DataPack<Data> onNoData(Param p, String key) throws DataException {
                // 加锁，避免并发时数据重复获取
                synchronized (CacheServiceNode.this.getLock(key)) {
                    return CacheServiceNode.this.getDataFromNextServiceOrDatasource(dataGroup, p, key, config);
                }
            }
        });
    }

    /**
     * 移除数据
     */
    void removeCache(String dataGroup, Param param) {
        CacheServiceNode<Param, Data> node = this;
        // 递归移除数据
        while (null != node) {
            node.mCurService.removeCache(dataGroup, param, node.mConverter.getKey(param));
            node = node.mNextNode;
        }
    }

    void clearCache(String dataGroup) {
        CacheServiceNode<Param, Data> node = this;
        // 递归移除数据
        while (null != node) {
            node.mCurService.clearCache(dataGroup);
            node = node.mNextNode;
        }
    }

    /**
     * 从下一节点或数据源获取数据
     */
    private DataPack<Data> getDataFromNextServiceOrDatasource(final String dataGroup, Param param, String key, final DataAccessConfig config) throws DataException {
        IOnNoDataListener<Param, Data> listener = new IOnNoDataListener<Param, Data>() {
            @Override
            public DataPack<Data> onNoData(Param param, String key) throws DataException {
                try {
                    // 临时排除禁止使用的缓存数据源
                    CacheServiceNode<Param, Data> nextNode = getNextAvailableNode();
                    // 有下一节点，则去下一节点获取；没有时，若允许访问数据源，则从数据源获取；否则抛出“没数据”异常
                    DataPack<Data> pack;
                    if (null != nextNode) {
                        pack = nextNode.getData(dataGroup, param, config);
                    } else if (null == config || config.canAccessSource) {
                        pack = DataPack.newSourceDataPack(mDatasource.onGetData(param));
                    } else {
                        throw new NoDataSourceException();
                    }
                    // 回调缓存接口
                    if (pack.canCache) {
                        mCurService.onCacheData(dataGroup, param, key, pack.data);
                    }
                    return pack;
                } catch (DataSourceException e) {
                    // 回调无数据接口
                    mCurService.onNoDataToCache(dataGroup, param, key);
                    throw e;
                } catch (CacheAntiPenetrateException e) {
                    mCurService.onNoDataToCache(dataGroup, param, key);
                    throw e;
                }
            }

            private CacheServiceNode<Param, Data> getNextAvailableNode() {
                CacheServiceNode<Param, Data> nextNode = null;
                if (null == config) {
                    nextNode = mNextNode;
                } else {
                    CacheServiceNode<Param, Data> tmpNode = mNextNode;
                    while (null != tmpNode) {
                        if (!config.tempExcludedServiceIds.contains(tmpNode.mCurService.getId())) {
                            nextNode = tmpNode;
                            break;
                        }
                        tmpNode = tmpNode.mNextNode;
                    }
                }
                return nextNode;
            }
        };
        // 若服务支持双重检查，则再次检查；否则直接执行回调
        if (mCurService.supportDoubleCheck()) {
            return getDataFromCurService(dataGroup, param, key, listener);
        } else {
            return listener.onNoData(param, key);
        }
    }

    /**
     * 从本地缓存服务获取数据
     */
    private DataPack<Data> getDataFromCurService(String dataGroup, Param param, String key, IOnNoDataListener<Param, Data> listener) throws DataException {
        try {
            return mCurService.onRetrieveCachedData(dataGroup, param, key);
        } catch (CacheNoDataException e) {
            return listener.onNoData(param, key);
        }
    }

    private synchronized String getLock(String key) {
        String result = mKeyMap.get(key);
        if (null == result) {
            mKeyMap.put(key, result = mCurService.toString() + key);
        }
        return result;
    }

    private interface IOnNoDataListener<Param, Data> {
        DataPack<Data> onNoData(Param param, String key) throws DataException;
    }
}
