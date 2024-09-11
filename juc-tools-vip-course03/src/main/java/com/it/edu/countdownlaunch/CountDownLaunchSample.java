package com.it.edu.countdownlaunch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**

 *
 * @author ：图灵-杨过
 * @date：2019/7/4
 * @version: V1.0
 * @slogan:天下风云出我辈，一入代码岁月催
 * @description： CountDownLatch示例
 *   main：主线程
 *     1.任务一：媳妇看大夫。         假设耗时2s
 *     2.任务二：同时，我去排队交钱。  假设耗时5s
 *     3.任务都完成后，回家。         全程耗时5s
 */
public class CountDownLaunchSample {

    public static void main(String[] args) throws InterruptedException {
        long now = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(2);//锁持有数
        new Thread(new SeeDoctorTask(countDownLatch)).start();
        new Thread(new QueueTask(countDownLatch)).start();
        //等待线程池中的2个任务执行完毕，否则一直阻塞
        countDownLatch.await();//尝试获取共享锁

        //所有的任务完成。则回家
        System.out.println("over，回家。 cost:"+(System.currentTimeMillis()-now));
    }
}
