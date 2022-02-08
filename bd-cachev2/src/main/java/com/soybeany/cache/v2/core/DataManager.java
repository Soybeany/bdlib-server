package com.soybeany.cache.v2.core;


import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.exception.BdCacheRtException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataCore;
import com.soybeany.cache.v2.model.DataPack;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.LinkedList;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
@SuppressWarnings("UnusedReturnValue")
@RequiredArgsConstructor
public class DataManager<Param, Data> {

    private final String dataDesc;
    private final IDatasource<Param, Data> defaultDatasource;
    private final IKeyConverter<Param> paramDescConverter;
    private final IKeyConverter<Param> paramKeyConverter;
    private final CacheNode<Param, Data> firstNode;
    private final ILogger<Param, Data> logger;

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
        return getDataPack(param, defaultDatasource);
    }

    /**
     * 获得数据(数据包方式)
     */
    public DataPack<Data> getDataPack(Param param, IDatasource<Param, Data> datasource) {
        // 没有缓存节点的情况
        if (null == firstNode) {
            return innerGetDataPackDirectly(param, datasource);
        }
        // 有缓存节点的情况
        DataContext<Param> context = getNewDataContext(param);
        DataPack<Data> pack = firstNode.getDataPackAndAutoCache(context, datasource);
        // 记录日志
        if (null != logger) {
            logger.onGetData(context, pack);
        }
        return pack;
    }

    /**
     * 直接从数据源获得数据(不使用缓存)
     */
    public DataPack<Data> getDataPackDirectly(Param param) {
        return innerGetDataPackDirectly(param, defaultDatasource);
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
    public void removeCache(Param param, int... cacheIndexes) {
        if (null == firstNode) {
            return;
        }
        DataContext<Param> context = getNewDataContext(param);
        firstNode.removeCache(context, cacheIndexes);
        // 记录日志
        if (null != logger) {
            logger.onRemoveCache(context, cacheIndexes);
        }
    }

    /**
     * 清除全部缓存(全部存储器)
     */
    public void clearCache(int... cacheIndexes) {
        if (null == firstNode) {
            return;
        }
        firstNode.clearCache(cacheIndexes);
        // 记录日志
        if (null != logger) {
            logger.onClearCache(dataDesc, cacheIndexes);
        }
    }

    // ********************内部方法********************

    private DataContext<Param> getNewDataContext(Param param) {
        String paramKey = paramKeyConverter.getKey(param);
        String paramDesc = paramKey;
        if (paramKeyConverter != paramDescConverter) {
            paramDesc = paramDescConverter.getKey(param);
        }
        return new DataContext<>(dataDesc, paramDesc, paramKey, param);
    }

    private DataPack<Data> innerGetDataPackDirectly(Param param, IDatasource<Param, Data> datasource) {
        DataPack<Data> pack = CacheNode.getDataDirectly(this, param, datasource);
        // 记录日志
        if (null != logger) {
            logger.onGetData(getNewDataContext(param), pack);
        }
        return pack;
    }

    private void innerCacheData(Param param, DataCore<Data> dataCore) {
        if (null == firstNode) {
            return;
        }
        DataContext<Param> context = getNewDataContext(param);
        DataPack<Data> pack = new DataPack<>(dataCore, this, Integer.MAX_VALUE);
        firstNode.cacheData(context, pack);
        // 记录日志
        if (null != logger) {
            logger.onCacheData(context, pack);
        }
    }

    // ********************内部类********************

    public static class Builder<Param, Data> {

        private final LinkedList<CacheNode<Param, Data>> mNodes = new LinkedList<>();
        private final String dataDesc;
        private final IDatasource<Param, Data> defaultDatasource;
        private final IKeyConverter<Param> paramKeyConverter;
        private IKeyConverter<Param> paramDescConverter;
        private ILogger<Param, Data> logger;

        public static <Data> Builder<String, Data> get(String dataDesc, IDatasource<String, Data> datasource) {
            return new Builder<>(dataDesc, datasource, new IKeyConverter.Std());
        }

        public static <Param, Data> Builder<Param, Data> get(String dataDesc, IDatasource<Param, Data> datasource, IKeyConverter<Param> keyConverter) {
            return new Builder<>(dataDesc, datasource, keyConverter);
        }

        private Builder(String dataDesc, IDatasource<Param, Data> datasource, IKeyConverter<Param> keyConverter) {
            if (null == keyConverter) {
                throw new BdCacheRtException("keyConverter不能为null");
            }
            this.dataDesc = dataDesc;
            this.defaultDatasource = datasource;
            this.paramKeyConverter = keyConverter;
            this.paramDescConverter = keyConverter;
        }

        // ********************设置********************

        /**
         * 使用缓存存储器，可以多次调用，形成多级缓存
         * <br>第一次调用为一级缓存，第二次为二级缓存...以此类推
         * <br>数据查找时一级缓存最先被触发
         * <br/>自定义storage时需注意，多级缓存，最终的优先级会逐级减一
         */
        public Builder<Param, Data> withCache(ICacheStorage<Param, Data> storage) {
            if (null == storage) {
                throw new BdCacheRtException("storage不能为null");
            }
            // 添加到存储器列表
            mNodes.add(new CacheNode<>(storage, storage.priority() - mNodes.size()));
            return this;
        }

        /**
         * 若需要记录日志，则配置该logger
         */
        public Builder<Param, Data> logger(ILogger<Param, Data> logger) {
            this.logger = logger;
            return this;
        }

        /**
         * 配置入参描述转换器
         * <br>* 根据入参定义自动输出的日志中使用的paramDesc
         * <br>* 默认使用构造时指定的“defaultConverter”
         */
        public Builder<Param, Data> paramDescConverter(IKeyConverter<Param> converter) {
            if (null != converter) {
                this.paramDescConverter = converter;
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
            CacheNode<Param, Data> firstNode = buildChain();
            // 返回管理器实例
            return new DataManager<>(dataDesc, defaultDatasource, paramDescConverter, paramKeyConverter, firstNode, logger);
        }

        // ********************内部方法********************

        private CacheNode<Param, Data> buildChain() {
            CacheNode<Param, Data> nextNode = null;
            for (CacheNode<Param, Data> node : mNodes) {
                node.setNextNode(nextNode);
                nextNode = node;
            }
            return nextNode;
        }

        /**
         * 用于缓存服务的排序器
         */
        private static class ServiceComparator implements Comparator<CacheNode<?, ?>> {
            @Override
            public int compare(CacheNode o1, CacheNode o2) {
                return o1.getPriority() - o2.getPriority();
            }
        }
    }

}
