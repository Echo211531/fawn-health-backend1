package com.zr.health.context;

import com.alibaba.ttl.TransmittableThreadLocal;

public class BaseContext {

    public static final ThreadLocal<Long> threadLocal = new TransmittableThreadLocal<>();

    /**
     * 设置用户id到threadLocal内存空间中
     * @param id
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 从threadLocal内存中获取用户id
     * @return
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
