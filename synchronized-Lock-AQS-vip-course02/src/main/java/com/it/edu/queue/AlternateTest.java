package com.it.edu.queue;


import com.it.edu.aqs.ReentrantLock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * ReentrantLock 条件队列的应用
 * 交替打印
 */
public class AlternateTest {

    public static void main(String[] args) {
        char[] number = "1234567".toCharArray();
        char[] abc = "ABCDEFG".toCharArray();


        Lock lock = new ReentrantLock();
        Condition conditionT1 = lock.newCondition();
        Condition conditionT2 = lock.newCondition();


        new Thread(() -> {
            try {
                lock.lock();    //如果t1线程拿到锁
                for (char c : number) {
                    System.out.print(c);  //t1执行打印，完成后，通知 t2 满足唤醒条件
                    conditionT2.signal(); //唤醒t2条件队列中的线程
                    conditionT1.await();  //t1线程阻塞，到条件队列;  注意： t1入条件队列前，会先释放锁
                }
                conditionT2.signal(); //t1全部打印完成后，t1再次唤醒 t2条件队列中的线程
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "t1").start();


        new Thread(() -> {
            try {
                lock.lock();    //如果t2线程拿到锁。
                for (char c : abc) {
                    System.out.print(c);  //t2执行打印，完成后，通知 t1 满足唤醒条件
                    conditionT1.signal(); //唤醒t1条件队列中的线程
                    conditionT2.await();  //t2线程阻塞到条件队列;  注意： t2入条件队列前，会先释放锁
                }
                conditionT1.signal(); //t2全部打印完成后，t2再次唤醒 t1条件队列中的线程
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }, "t2").start();


    }

}
