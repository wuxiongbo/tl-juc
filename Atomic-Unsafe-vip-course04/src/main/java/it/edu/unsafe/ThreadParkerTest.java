package it.edu.unsafe;

import java.util.concurrent.locks.LockSupport;

/**
 * @author ：图灵-杨过
 * @date：2019/8/2
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : unsafe之 阻塞线程、唤醒线程
 */
public class ThreadParkerTest {

    public static void main(String[] args) {

        /*Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("thread - is running----");
                LockSupport.park();//阻塞当前线程
                System.out.println("thread is over-----");
            }
        });

        t.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LockSupport.unpark(t);//唤醒指定的线程*/

        //2.拿出票据使用
        LockSupport.park();
        System.out.println("main thread is over");

        //1.相当于先往池子里放了一张票据
        LockSupport.unpark(Thread.currentThread());//Pthread_mutex
        System.out.println("im running step 1");

        //以2、1 顺序执行，拿不出票据，线程会在LockSupport.park();这里阻塞
    }

}
