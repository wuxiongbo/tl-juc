package com.it.edu.countdownlaunch;

import java.util.concurrent.CountDownLatch;

/**
 * @author ：图灵-杨过
 * @date：2019/7/11
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : 任务二：排队
 */
public class QueueTask implements Runnable {

    private CountDownLatch countDownLatch;

    public QueueTask(CountDownLatch countDownLatch){
        this.countDownLatch = countDownLatch;
    }
    public void run() {
        try {
            System.out.println("Task2：开始在医院药房排队买药....");
            Thread.sleep(5000);
            System.out.println("Task2：排队完成，开始缴费买药");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if (countDownLatch != null)
                countDownLatch.countDown(); //锁计数减1
        }
    }
}
