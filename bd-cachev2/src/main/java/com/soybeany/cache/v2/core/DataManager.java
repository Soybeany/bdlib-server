package com.soybeany.cache.v2.core;


import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataManager<Param, Data> {

    private final IDatasource<Param, Data> mDefaultDatasource;

    private CacheNode<Param, Data> mFirstNode; // 调用链的头

    /**
     * @param defaultDatasource 默认的数据源，允许为null
     */
    private DataManager(IDatasource<Param, Data> defaultDatasource) {
        mDefaultDatasource = defaultDatasource;
    }

    // ********************操作********************

    /**
     * 获得数据(默认方式)
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public Data getData(Param param) throws DataException {
        return getDataPack(param).data;
    }

    /**
     * 获得数据(默认方式)
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public Data getData(Param param, IDatasource<Param, Data> datasource) throws DataException {
        return getDataPack(param, datasource).data;
    }

    /**
     * 获得数据(数据包方式)
     */
    public DataPack<Data> getDataPack(Param param) throws DataException {
        return getDataPack(param, mDefaultDatasource);
    }

    /**
     * 获得数据(数据包方式)
     */
    public DataPack<Data> getDataPack(Param param, IDatasource<Param, Data> datasource) throws DataException {
        // 没有缓存节点的情况
        if (null == mFirstNode) {
            if (null == datasource) {
                throw new DataException(DataFrom.SOURCE, new NoDataSourceException());
            }
            return getDataPackDirectly(param);
        }
        // 有缓存节点的情况
        return mFirstNode.getCache(param, datasource);
    }

    /**
     * 直接从数据源获得数据(不使用缓存)
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public DataPack<Data> getDataPackDirectly(Param param) throws DataException {
        return CacheNode.getDataDirectly(param, mDefaultDatasource);
    }

    /**
     * 缓存数据，手动模式管理
     */
    public void cacheData(Param param, DataPack<Data> data) {
        if (null != mFirstNode) {
            mFirstNode.cacheData(param, data);
        }
    }

    /**
     * 缓存异常，手动模式管理
     */
    public void cacheException(Param param, Exception e) {
        if (null != mFirstNode) {
            mFirstNode.cacheException(param, e);
        }
    }

    /**
     * 移除指定key的缓存(全部策略)
     *
     * @param param 用于匹配数据
     */
    public void removeCache(Param param) {
        if (null != mFirstNode) {
            mFirstNode.removeCache(param);
        }
    }

    /**
     * 清除全部缓存(全部策略)
     */
    public void clearCache() {
        if (null != mFirstNode) {
            mFirstNode.clearCache();
        }
    }

    // ********************内部类********************

    public static class Builder<Param, Data> {

        private final DataManager<Param, Data> mManager;
        private final IKeyConverter<Param> mDefaultConverter;
        private final LinkedList<CacheNode<Param, Data>> mNodes = new LinkedList<CacheNode<Param, Data>>();

        public static <Data> Builder<String, Data> get(IDatasource<String, Data> datasource) {
            return new Builder<String, Data>(datasource, new IKeyConverter.Std());
        }

        public static <Param, Data> Builder<Param, Data> get(IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            return new Builder<Param, Data>(datasource, defaultConverter);
        }

        private Builder(IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            mManager = new DataManager<Param, Data>(datasource);
            mDefaultConverter = defaultConverter;
        }

        // ********************设置********************

        /**
         * 使用缓存策略，可以多次调用，形成多级缓存
         * <br>第一次调用为一级缓存，第二次为二级缓存...以此类推
         * <br>数据查找时一级缓存最先被触发
         */
        public Builder<Param, Data> withCache(ICacheStrategy<Param, Data> strategy) {
            return withCache(strategy, null);
        }

        /**
         * 与{@link #withCache(ICacheStrategy)}相同，只是允许自定义KeyConverter
         */
        public Builder<Param, Data> withCache(ICacheStrategy<Param, Data> strategy, IKeyConverter<Param> converter) {
            if (null == converter) {
                converter = mDefaultConverter;
            }
            // 添加到服务列表
            mNodes.addFirst(new CacheNode<Param, Data>(strategy, converter));
            return this;
        }

        /**
         * 构建出用于使用的实例
         */
        public DataManager<Param, Data> build() {
            // 节点排序
            Collections.sort(mNodes, new ServiceComparator());
            // 创建调用链
            buildChain();
            // 返回管理器实例
            return mManager;
        }

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
                return o1.getStrategy().order() - o2.getStrategy().order();
            }
        }
    }

}
