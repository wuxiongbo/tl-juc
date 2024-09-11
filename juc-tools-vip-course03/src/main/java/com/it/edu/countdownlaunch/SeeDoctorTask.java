package com.it.edu.countdownlaunch;

import java.util.concurrent.CountDownLatch;

/**
 * @author ：图灵-杨过
 * @date：2019/7/11
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : 任务一：看大夫
 */
public class SeeDoctorTask implements Runnable {
    private CountDownLatch countDownLatch;

    public SeeDoctorTask(CountDownLatch countDownLatch){
        this.countDownLatch = countDownLatch;
    }

    public void run() {
        try {
            System.out.println("Task1：开始看医生");
            Thread.sleep(2000);
            System.out.println("Task1：看医生结束，准备离开病房");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            if (countDownLatch != null)
                countDownLatch.countDown(); //锁计数减1
        }
    }

}
