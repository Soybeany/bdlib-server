package com.soybeany.cache.v2.core;


import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataCore;
import com.soybeany.cache.v2.model.DataPack;

import java.util.Comparator;
import java.util.LinkedList;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
@SuppressWarnings("UnusedReturnValue")
public class DataManager<Param, Data> {

    private final String mDataDesc;
    private final IDatasource<Param, Data> mDefaultDatasource;

    private CacheNode<Param, Data> mFirstNode; // 调用链的头
    private ILogger<Param, Data> mLogger; // 日志输出
    private IKeyConverter<Param> mParamDescConverter; // 入参描述转换器

    /**
     * @param defaultDatasource 默认的数据源，允许为null
     */
    private DataManager(String dataDesc, IDatasource<Param, Data> defaultDatasource) {
        mDataDesc = dataDesc;
        mDefaultDatasource = defaultDatasource;
    }

    // ********************操作********************

    /**
     * 获得数据(默认方式)
     */
    public Data getData(Param param) throws Exception {
        return getDataPack(param).getData();
    }

    /**
     * 获得数据(数据包方式)
     */
    public DataPack<Data> getDataPack(Param param) {
        return getDataPack(param, mDefaultDatasource);
    }

    /**
     * 获得数据(数据包方式)
     */
    public DataPack<Data> getDataPack(Param param, IDatasource<Param, Data> datasource) {
        // 没有缓存节点的情况
        if (null == mFirstNode) {
            return innerGetDataPackDirectly(param, datasource);
        }
        // 有缓存节点的情况
        DataContext<Param> context = getNewDataContext(param);
        DataPack<Data> pack = mFirstNode.getDataPackAndAutoCache(context, datasource);
        // 记录日志
        if (null != mLogger) {
            mLogger.onGetData(context, pack);
        }
        return pack;
    }

    /**
     * 直接从数据源获得数据(不使用缓存)
     */
    public DataPack<Data> getDataPackDirectly(Param param) {
        return innerGetDataPackDirectly(param, mDefaultDatasource);
    }

    /**
     * 缓存数据，手动模式管理
     */
    public void cacheData(Param param, Data data) {
        innerCacheData(param, DataCore.fromData(data));
    }

    /**
     * 缓存异常，手动模式管理
     */
    public void cacheException(Param param, Exception e) {
        innerCacheData(param, DataCore.fromException(e));
    }

    /**
     * 移除指定key在指定存储器中的缓存
     */
    public void removeCache(Param param, int... storageIndexes) {
        if (null == mFirstNode) {
            return;
        }
        DataContext<Param> context = getNewDataContext(param);
        mFirstNode.removeCache(context, storageIndexes);
        // 记录日志
        if (null != mLogger) {
            mLogger.onRemoveCache(context, storageIndexes);
        }
    }

    /**
     * 移除指定key的陈旧缓存(全部存储器中)，即要求数据的剩余有效时间大于指定值
     */
    public void removeOldCache(Param param, int validMillisAtLease) {
        if (null == mFirstNode) {
            return;
        }
        DataContext<Param> context = getNewDataContext(param);
        int removeLevel = mFirstNode.removeOldCache(context, validMillisAtLease);
        // 记录日志
        if (null != mLogger) {
            mLogger.onRemoveOldCache(context, removeLevel);
        }
    }

    /**
     * 清除全部缓存(全部存储器)
     */
    public void clearCache(int... storageIndexes) {
        if (null == mFirstNode) {
            return;
        }
        mFirstNode.clearCache(mDataDesc, storageIndexes);
        // 记录日志
        if (null != mLogger) {
            mLogger.onClearCache(mDataDesc, storageIndexes);
        }
    }

    // ********************内部方法********************

    private DataContext<Param> getNewDataContext(Param param) {
        return new DataContext<>(mDataDesc, mParamDescConverter.getKey(param), param);
    }

    private DataPack<Data> innerGetDataPackDirectly(Param param, IDatasource<Param, Data> datasource) {
        DataPack<Data> pack = CacheNode.getDataDirectly(this, param, datasource);
        // 记录日志
        if (null != mLogger) {
            mLogger.onGetData(getNewDataContext(param), pack);
        }
        return pack;
    }

    private void innerCacheData(Param param, DataCore<Data> dataCore) {
        if (null == mFirstNode) {
            return;
        }
        DataContext<Param> context = getNewDataContext(param);
        DataPack<Data> pack = new DataPack<>(dataCore, this, Integer.MAX_VALUE);
        mFirstNode.cacheData(context, pack);
        // 记录日志
        if (null != mLogger) {
            mLogger.onCacheData(context, pack);
        }
    }

    // ********************内部类********************

    public static class Builder<Param, Data> {

        private final DataManager<Param, Data> mManager;
        private final IKeyConverter<Param> mDefaultConverter;
        private final LinkedList<CacheNode<Param, Data>> mNodes = new LinkedList<>();

        public static <Data> Builder<String, Data> get(String dataDesc, IDatasource<String, Data> datasource) {
            return new Builder<>(dataDesc, datasource, new IKeyConverter.Std());
        }

        public static <Param, Data> Builder<Param, Data> get(String dataDesc, IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            return new Builder<>(dataDesc, datasource, defaultConverter);
        }

        private Builder(String dataDesc, IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            mManager = new DataManager<>(dataDesc, datasource);
            mDefaultConverter = defaultConverter;
            mManager.mParamDescConverter = defaultConverter;
        }

        // ********************设置********************

        /**
         * 使用缓存存储器，可以多次调用，形成多级缓存
         * <br>第一次调用为一级缓存，第二次为二级缓存...以此类推
         * <br>数据查找时一级缓存最先被触发
         */
        public Builder<Param, Data> withCache(ICacheStorage<Param, Data> storage) {
            if (null == storage) {
                throw new RuntimeException("storage不能为null");
            }
            // 按需为存储器设置转换器
            IKeyConverter<Param> converter = storage.getConverter();
            if (null == converter) {
                storage = storage.converter(mDefaultConverter);
            }
            // 添加到存储器列表
            mNodes.addFirst(new CacheNode<>(storage));
            return this;
        }

        /**
         * 若需要记录日志，则配置该logger
         */
        public Builder<Param, Data> logger(ILogger<Param, Data> logger) {
            mManager.mLogger = logger;
            return this;
        }

        /**
         * 配置入参描述转换器
         * <br>* 根据入参定义自动输出的日志中使用的paramDesc
         * <br>* 默认使用构造时指定的“defaultConverter”
         */
        public Builder<Param, Data> paramDescConverter(IKeyConverter<Param> converter) {
            if (null != converter) {
                mManager.mParamDescConverter = converter;
            }
            return this;
        }

        /**
         * 构建出用于使用的实例
         */
        public DataManager<Param, Data> build() {
            // 节点排序
            mNodes.sort(new ServiceComparator());
            // 创建调用链
            buildChain();
            // 返回管理器实例
            return mManager;
        }

        // ********************内部方法********************

        private void buildChain() {
            CacheNode<Param, Data> nextNode = null;
            for (CacheNode<Param, Data> node : mNodes) {
                node.setNextNode(nextNode);
                nextNode = node;
            }
            mManager.mFirstNode = nextNode;
        }

        /**
         * 用于缓存服务的排序器
         */
        private static class ServiceComparator implements Comparator<CacheNode<?, ?>> {
            @Override
            public int compare(CacheNode o1, CacheNode o2) {
                return o1.getStorage().order() - o2.getStorage().order();
            }
        }
    }

}
