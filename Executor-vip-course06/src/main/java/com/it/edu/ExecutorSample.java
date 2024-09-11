package com.it.edu;

//import java.util.concurrent.*;

import com.it.edu.task.RunTask;
import com.it.edu.threadpool.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ：图灵-杨过
 * @date：2019/7/18
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description :
 */
public class ExecutorSample {
    @Test
    void test0() {
        //创建定时线程池。底层实现是newScheduledThreadPool类
        //ExecutorService executor = Executors.newScheduledThreadPool(int corePoolSize);

        //创建默认线程池。底层实现是ThreadPoolExecutor类,所以本质上是创建了一个ThreadPoolExecutor线程池类
        //ExecutorService executor = Executors.newFixedThreadPool(5);
        ExecutorService executor =
                new ThreadPoolExecutor(5, 5,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());

        for (int i=0;i<20;i++){
            executor.execute(new RunTask());
//            executor.submit(new RunTask());
        }

//        executor.shutdownNow();//执行该方法进入stop状态
//        executor.shutdown();//执行该方法进入SHUTDOWN状态

//        executor.execute(new RunTask());//SHUTDOWN状态，不接收新任务，但能处理已添加的任务。
    }
    /*
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-1
Thread name:pool-1-thread-2
Thread name:pool-1-thread-3
Thread name:pool-1-thread-4
Thread name:pool-1-thread-5*/
//上面代码的运行结果 验证了线程的创建过程比较慢、耗资源


    ExecutorService init(){
        return Executors.newFixedThreadPool(2);
    }

    @Test
    // shutdown
    void test1(){
        ExecutorService executor = init();
        for (int i = 0; i < 5; i++) {
            // lambda表达式内只能使用final变量，所以这里使用具有相同效果的String类
            String str = i + "";
            executor.execute(() -> System.out.println(str));
        }
        executor.shutdown();
        System.out.println("线程池已关闭");
    }

    @Test
    // shutdown后，继续添加任务
    void test2(){
        ExecutorService executor = init();
        for (int i = 0; i < 5; i++) {
            String str = i + "";
            executor.execute(() -> System.out.println(str));
        }
        executor.shutdown();

        //调用shutdown()方法后继续添加任务
        executor.execute(() -> System.out.println("ok"));

        System.out.println("线程池已关闭");
    }

    @Test
    // shutdown后，阻塞main线程，不让main线程消亡。
    void test3(){
        ExecutorService executor = init();
        for (int i = 0; i < 5; i++) {
            String str = i + "";
            executor.execute(() -> System.out.println(str));
        }
        // shutdown() 和 awaitTermination() 组合使用。
        executor.shutdown();

        try {
            executor.awaitTermination(1, TimeUnit.MINUTES); //阻塞main线程
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("线程池已关闭");
    }

}

