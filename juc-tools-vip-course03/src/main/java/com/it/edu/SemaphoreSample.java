package com.it.edu;


import com.it.edu.aqs.Semaphore;

/**
 * @author ：图灵-杨过
 * @date：2019/7/11
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : Semaphore
 *                可用于流量控制，限制最大的并发访问数
 */
public class SemaphoreSample  implements Runnable{

    Semaphore semaphore;

    public SemaphoreSample(Semaphore semaphore){
        this.semaphore = semaphore;
    }

    public void run() {
        try {
            semaphore.acquire(); // 拿锁
            System.out.println(Thread.currentThread().getName()+":==acquire== at time:"+System.currentTimeMillis());

            Thread.sleep(2000);

            semaphore.release(); // 释放锁
            System.out.println(Thread.currentThread().getName()+":release at time:"+System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(2);

        // 启动5个线程。
        // 开始时，只有两个线程能拿到锁。
        // 剩下的3个线程阻塞在CLH队列中。
        for (int i=0;i<5;i++){
            Thread temp = new Thread(new SemaphoreSample(semaphore));
            temp.setName("yangguo+"+i);
            temp.start();
        }

    }

}
