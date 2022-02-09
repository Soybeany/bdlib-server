package com.soybeany.cache.v2.storage;

import com.soybeany.cache.v2.contract.ICacheStorage;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Soybeany
 * @date 2022/2/8
 */
@Accessors(fluent = true, chain = true)
public abstract class StdStorageBuilder<Param, Data> {

    /**
     * 正常数据的生存时间，用于一般场景
     */
    @Setter
    protected int pTtl = Integer.MAX_VALUE;

    /**
     * 异常的生存时间，用于防缓存穿透等场景
     */
    @Setter
    protected int pTtlErr = 60000;

    public ICacheStorage<Param, Data> build() {
        // 预处理时间
        handleTtl();
        // 构建
        return onBuild();
    }

    // ***********************子类重新****************************

    protected abstract ICacheStorage<Param, Data> onBuild();

    // ***********************内部方法****************************

    private void handleTtl() {
        pTtl = Math.max(pTtl, 1);
        pTtlErr = Math.max(pTtlErr, 1);
        if (pTtlErr > pTtl) {
            pTtlErr = pTtl;
        }
    }

}
