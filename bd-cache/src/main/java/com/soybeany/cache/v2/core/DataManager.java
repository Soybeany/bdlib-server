package com.soybeany.cache.v2.core;


import com.google.gson.Gson;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.module.DataGetMode;
import com.soybeany.cache.v2.module.DataPack;

import java.util.*;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataManager<Param, Data> {

    private static final Gson GSON = new Gson();

    private final String mDesc;
    private final IDatasource<Param, Data> mDatasource;
    private final List<IDataOperationListener<Param, Data>> mListeners = new LinkedList<>();

    private CacheServiceNode<Param, Data> mFirstNode; // 调用链的头

    private DataManager(String desc, IDatasource<Param, Data> datasource) {
        if (null == desc || desc.isEmpty()) {
            throw new RuntimeException("请填写描述");
        }
        if (null == datasource) {
            throw new RuntimeException("数据源不能为null");
        }
        mDesc = desc;
        mDatasource = datasource;
    }

    // ********************操作********************

    /**
     * 获得数据(默认方式)
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public Data getData(final Param param) throws DataException {
        return innerGetData(param, new IGetDataHandler<>() {
            @Override
            public Data onNoCacheService() throws DataException {
                // 若没有设置缓存服务，则直接访问数据源
                return getDataDirectly(param);
            }

            @Override
            public DataAccessConfig onGetDataAccessConfig() {
                return null;
            }

            @Override
            public DataGetMode onGetMode() {
                return DataGetMode.NORM;
            }
        });
    }

    /**
     * 直接从数据源获得数据(不使用缓存)
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public Data getDataDirectly(Param param) throws DataException {
        Data data = mDatasource.onGetData(param);
        // 执行回调
        for (IDataOperationListener<Param, Data> listener : mListeners) {
            listener.onGetData(mDesc, DataGetMode.DIRECT, param, DataPack.newSourceDataPack(data, mDatasource));
        }
        // 返回数据
        return data;
    }

    /**
     * 从数据源重新获取数据
     *
     * @param param 用于匹配数据
     * @return 相匹配的数据
     */
    public Data updateCache(Param param) throws DataException {
        try {
            removeCache(param);
            Data data = getData(param);
            // 执行回调
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onUpdateCache(mDesc, param, data);
            }
            // 返回数据
            return data;
        } catch (Exception e) {
            return onHandleException(param, e);
        }
    }

    /**
     * 移除指定key的缓存(全部service)
     *
     * @param param 用于匹配数据
     */
    public void removeCache(Param param) {
        try {
            if (null != mFirstNode) {
                mFirstNode.removeCache(mDesc, param);
            }
            // 执行回调
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onRemoveCache(mDesc, param);
            }
        } catch (Exception e) {
            onHandleUnexpectException(e);
        }
    }

    /**
     * 清除全部缓存(全部service)
     */
    public void clearCache() {
        try {
            if (null != mFirstNode) {
                mFirstNode.clearCache(mDesc);
            }
            // 执行回调
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onClearCache(mDesc);
            }
        } catch (Exception e) {
            onHandleUnexpectException(e);
        }
    }

    // ********************内部方法********************

    private Data innerGetData(Param param, IGetDataHandler<Data> handler) throws DataException {
        if (null == mFirstNode) {
            return handler.onNoCacheService();
        }
        try {
            DataPack<Data> pack = mFirstNode.getData(mDesc, param, handler.onGetDataAccessConfig());
            // 执行回调
            DataGetMode mode = handler.onGetMode();
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onGetData(mDesc, mode, param, pack);
            }
            // 返回数据
            return pack.data;
        } catch (Exception e) {
            return onHandleException(param, e);
        }
    }

    private Data onHandleException(Param param, Exception e) throws DataException {
        if (e instanceof DataException) {
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onNoData(mDesc, param);
            }
            throw (DataException) e;
        }
        onHandleUnexpectException(e);
        throw new RuntimeException("出现了不可能的情况");
    }

    private void onHandleUnexpectException(Exception e) {
        if (e instanceof RuntimeException) {
            for (IDataOperationListener<Param, Data> listener : mListeners) {
                listener.onHandleUnexpectException(mDesc, e);
            }
            throw (RuntimeException) e;
        }
    }

    // ********************内部类********************

    public static class Builder<Param, Data> {

        private final DataManager<Param, Data> mManager;
        private final IKeyConverter<Param> mDefaultConverter;
        private final List<CacheServiceNode<Param, Data>> mNodes = new LinkedList<>();
        private final Set<String> mIds = new HashSet<>();

        /**
         * @param desc 描述此数据管理器，如“存放了些什么数据”
         */
        public static <Data> Builder<String, Data> get(String desc, IDatasource<String, Data> datasource) {
            return new Builder<>(desc, datasource, new IKeyConverter.Std());
        }

        /**
         * @param desc 描述此数据管理器，如“存放了些什么数据”
         */
        public static <Param, Data> Builder<Param, Data> get(String desc, IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            return new Builder<>(desc, datasource, defaultConverter);
        }

        private Builder(String desc, IDatasource<Param, Data> datasource, IKeyConverter<Param> defaultConverter) {
            mManager = new DataManager<>(desc, datasource);
            mDefaultConverter = defaultConverter;
        }

        // ********************设置********************

        /**
         * 使用缓存策略，可以多次调用，形成多级缓存
         * <br>第一次调用为一级缓存，第二次为二级缓存...以此类推
         * <br>数据查找时一级缓存最先被触发
         */
        public Builder<Param, Data> withCache(ICacheService<Param, Data> service) {
            return withCache(service, null);
        }

        /**
         * 与{@link #withCache(ICacheService)}相同，只是允许自定义KeyConverter
         */
        public Builder<Param, Data> withCache(ICacheService<Param, Data> service, IKeyConverter<Param> converter) {
            if (null == converter) {
                converter = mDefaultConverter;
            }
            // 添加到服务列表
            mNodes.add(new CacheServiceNode<>(service, converter, mManager.mDatasource));
            // 判断id是否已存在
            String id = service.getId();
            if (mIds.contains(id)) {
                throw new RuntimeException("不能使用相同ID的缓存服务");
            }
            mIds.add(id);
            return this;
        }

        /**
         * 设置监听器，可以多次调用，按添加的顺序回调
         */
        @SuppressWarnings("UnusedReturnValue")
        public Builder<Param, Data> withListener(IDataOperationListener<Param, Data> listener) {
            if (null != listener) {
                mManager.mListeners.add(listener);
            }
            return this;
        }

        /**
         * 构建出用于使用的实例
         */
        public DataManager<Param, Data> build() {
            // 服务排序并执行回调
            sortAndHandleNodes();
            // 返回管理器实例
            return mManager;
        }

        private void sortAndHandleNodes() {
            // 节点排序
            mNodes.sort(new ServiceComparator());
            // 节点遍历处理
            CacheServiceNode<Param, Data> lastNode = null;
            for (CacheServiceNode<Param, Data> node : mNodes) {
                ICacheService<Param, Data> curService = node.getService();
                // 创建链
                if (null == lastNode) {
                    mManager.mFirstNode = node;
                } else {
                    lastNode.setNextNode(node);
                }
                lastNode = node;
                // 执行回调
                for (IDataOperationListener<Param, Data> listener : mManager.mListeners) {
                    listener.onApplyService(mManager.mDesc, curService);
                }
            }
        }

        /**
         * 用于缓存服务的排序器
         */
        private static class ServiceComparator implements Comparator<CacheServiceNode<?, ?>> {
            @Override
            public int compare(CacheServiceNode o1, CacheServiceNode o2) {
                return o2.getService().order() - o1.getService().order();
            }
        }
    }

    // ********************内部类********************

    public static class Container<Param, Data> {
        public final DataManager<Param, Data> manager;
        private final Class<Param> mParamClazz;

        Container(DataManager<Param, Data> manager, Class<Param> paramClazz) {
            this.manager = manager;
            mParamClazz = paramClazz;
        }

        public Param toParam(String json) {
            return GSON.fromJson(json, mParamClazz);
        }
    }

    private interface IGetDataHandler<Data> {
        Data onNoCacheService() throws DataException;

        DataAccessConfig onGetDataAccessConfig();

        DataGetMode onGetMode();
    }
}
