package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.exception.DataSourceException;

/**
 * 数据源
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public interface IDatasource<Param, Data> extends IDataProvider {
    /**
     * 从数据源获取数据
     *
     * @param param 请求参数
     * @return 数据
     * @throws DataSourceException 数据源相关的异常
     */
    Data onGetData(Param param) throws DataSourceException;
}
