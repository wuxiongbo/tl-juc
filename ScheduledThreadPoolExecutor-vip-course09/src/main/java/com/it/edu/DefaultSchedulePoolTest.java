package com.it.edu;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ：图灵-杨过
 * @date：2019/8/14
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : ScheduledThreadPoolExecutor
 */
public class DefaultSchedulePoolTest {

    public static void main(String[] args) {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(5);

        /*pool.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("延迟执行");
            }
        },1, TimeUnit.SECONDS);*/

        /**
         * 这个执行周期是固定，不管任务执行多长时间，每过3秒中就会产生一个新的任务
         */
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //这个业务逻辑需要很长的时间，如：定时任务去统计一张数据上亿的表，财务信息
                //30
                System.out.println("重复执行");
            }
        },1,3,TimeUnit.SECONDS);

        pool.shutdown();

        /**
         * 12点整，
         * 执行第一次任务。初始延时1s ，12:00 01s 时刻真正开始执行
         *    执行完第一次任务的时刻  12:30 01s
         *
         * 12:30 01s，
         * 执行第二次任务。延时3s，12:30 04s 时刻真正开始执行
         *    执行完第二次任务的时刻  13:00 04s
         */
        pool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //假设过程消耗 30min 整
                System.out.println("重复执行");
            }
        },1,3,TimeUnit.SECONDS);


    }

}
