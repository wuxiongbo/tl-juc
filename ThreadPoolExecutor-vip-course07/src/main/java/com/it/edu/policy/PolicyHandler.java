package com.it.edu.policy;

import com.it.edu.executor.TulingThreadPoolExecutor;

/**
 * @author ：图灵-杨过
 * @date：2019/7/22
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : 策略处理器
 */
public interface PolicyHandler {

    /**
     * 拒绝策略
     * @param task
     * @param executor
     */
    void rejected(Runnable task, TulingThreadPoolExecutor executor);

}
