package com.it.edu.sample;

//import java.util.concurrent.locks.ReentrantLock;

import com.it.edu.aqs.ReentrantLock;

/**
 * ReentrantLock 可重入锁
 */
public class ReentrantLockSample {
    //共享资源。假设counter表示，一个空池子被加过几桶水。
    private Integer counter = 0;
    /**
     * 1.可重入锁,
     * 2.公平锁，非公平锁
     * 3.需要保证多个线程使用的是同一个锁
     * <p>
     * synchronized是否可重入？
     * 虚拟机在ObjectMonitor.hpp中定义了synchronized它怎么取重入加锁 ..。hotspot源码
     * 每加锁一次，counter +1
     * 基于AQS 去实现加锁与解锁
     * <p>
     *
     * 源码：
     * public ReentrantLock(boolean fair) {
     *     sync = fair ? new FairSync() : new NonfairSync();
     * }
     * FairSync 公平锁，NonfairSync 非公平锁
     * 公平与非公平，就像 排队 与 插队 的区别
     */
    private ReentrantLock reentrantLock = new ReentrantLock(true);

    /**
     * 需要保证多个线程使用的是同一个ReentrantLock对象
     *
     * @return
     */
    public void modifyResources(String threadName) {
        System.out.println("通知《管理员》线程:===>" + threadName + "准备打水");


        //默认创建的是 独占锁(排它锁)；
        // 独占锁在同一时刻只允许一个线程获取 锁 进行 数据的读/写。
        // 线程A拿到锁。线程B则在这里阻塞。


        reentrantLock.lock();
        System.out.println("线程:--->" + threadName + "第一次加锁");
        counter++;
        System.out.println("线程:" + threadName + "打完第" + counter + "桶水");



        // 线程A拿到锁，所以可以重入该锁,我还有一件事情要做,没做完之前不能把锁资源让出去
        reentrantLock.lock();
        System.out.println("线程:--->" + threadName + "第二次加锁");
        counter++;
        System.out.println("线程:" + threadName + "打完第" + counter + "桶水");


        reentrantLock.unlock();
        System.out.println("线程:<---" + threadName + "释放一个锁");


        reentrantLock.unlock();
        System.out.println("线程:<---" + threadName + "再释放一个锁");
    }


    public static void main(String[] args) {
        ReentrantLockSample tp = new ReentrantLockSample();




        new Thread(() -> {
            String threadName = Thread.currentThread().getName();
            tp.modifyResources(threadName);
        }, " “Thread:杨过” ").start();



        new Thread(()->{
            String threadName = Thread.currentThread().getName();
            tp.modifyResources(threadName);
        }," “Thread:小龙女” ").start();


    }

    /**
     *运行结果：
     * 通知《管理员》线程:===> “Thread:杨过” 准备打水
     * 线程:---> “Thread:杨过” 第一次加锁
     * 线程: “Thread:杨过” 打完第1桶水
     * 线程:---> “Thread:杨过” 第二次加锁
     * 线程: “Thread:杨过” 打完第2桶水
     * 通知《管理员》线程:===> “Thread:小龙女” 准备打水  //注意这里，线程小龙女进来了，但是无法对共享数据进行操作。
     *
     * 线程:<--- “Thread:杨过” 释放一个锁
     * 线程:<--- “Thread:杨过” 再释放一个锁
     *
     * 线程:---> “Thread:小龙女” 第一次加锁            //注意这里，线程杨过将锁全部释放完毕，线程小龙女才有资格获取锁
     * 线程: “Thread:小龙女” 打完第3桶水
     * 线程:---> “Thread:小龙女” 第二次加锁
     * 线程: “Thread:小龙女” 打完第4桶水
     *
     * 线程:<--- “Thread:小龙女” 释放一个锁
     * 线程:<--- “Thread:小龙女” 再释放一个锁
     *
     *总结：
     * 写锁(独享锁、排它锁)，一次只能被一个线程所持有。
     * 如果“线程杨过”对‘共享数据count’加上‘排它锁’后，其他线程，如“线程小龙女”，则不能再对count加任何类型的锁，
     * 直到“线程杨过”的子流程都执行完毕，并释放锁，“线程小龙女”才能获取到锁。
     * 注意：加了几次锁，就需要释放几次锁。否则，上例中的“线程小龙女”会一直阻塞。
     * 获得写锁的线程即能 读数据 又能 修改数据。
     * */

}
