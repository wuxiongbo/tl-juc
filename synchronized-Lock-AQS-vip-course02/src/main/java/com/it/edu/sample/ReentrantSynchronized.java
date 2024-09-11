package com.it.edu.sample;

import org.openjdk.jol.info.ClassLayout;

/**
 *  验证 synchronized是 可重入锁。
 * @author wxb
 */
public class ReentrantSynchronized {

    private final static Object object = new Object();

    public static void reentrantLock(){
        synchronized (object) {
            System.out.println("message Info:--->进入第一个同步块");
            System.out.println(ClassLayout.parseInstance(object).toPrintable());
            synchronized (object){
                System.out.println("message Info:--->进入第二个同步块");
                System.out.println(ClassLayout.parseInstance(object).toPrintable());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantSynchronized.reentrantLock();
    }

}
