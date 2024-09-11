package com.it.edu;

import com.it.edu.task.RunTask;
import com.it.edu.threadpool.ThreadPoolExecutor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 线程池拒绝策略
 *
 * @author ：图灵-杨过
 * @date：2019/7/18
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : 线程池拒绝策略
 */
public class PolicySample {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                3,                                //arg1:corePoolSize
                5,                                           //arg2:maximumPoolSize
                3,                                           //arg3:keepAliveTime
                TimeUnit.SECONDS,                            //arg4:TimeUnit
                new ArrayBlockingQueue<Runnable>(2), //arg5:BlockingQueue
                Executors.defaultThreadFactory(),            //arg6:ThreadFactory
                new ThreadPoolExecutor.AbortPolicy()         //arg7:RejectedExecutionHandler
        );

        for (int i=0;i<20;i++){
            System.out.println(i);
            pool.submit(new RunTask());
        }
        //3+2+2=7 提交到第8个任务(索引为7)时，main抛异常终止。
        //此时，线程池中还有5个子线程。 以及7个任务(5个初始任务，2个队列任务)
        //核心线程数有3个，此时3个线程均保活阻塞在队列中。


        /*Future<String> future = null;
        List<Future<String>> list = new ArrayList<Future<String>>();

        for (int i=0;i<20;i++){
            future = pool.submit(new CallTask());
            list.add(future);
        }

        if(!list.isEmpty()){
            for (Future<String> f : list){
                System.out.println(f.get());
            }
        }*/
    }
}


