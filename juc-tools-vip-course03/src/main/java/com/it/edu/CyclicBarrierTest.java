package com.it.edu;

import java.util.concurrent.CyclicBarrier;

/**
 * @author ：图灵-杨过
 * @date：2019/7/15
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : CyclicBarrier栅栏屏障
 */
public class CyclicBarrierTest implements Runnable {
    private CyclicBarrier cyclicBarrier;
    private int index ;

    public CyclicBarrierTest(CyclicBarrier cyclicBarrier, int index) {
        this.cyclicBarrier = cyclicBarrier;
        this.index = index;
    }

    public void run() {
        try {
            System.out.println("index: " + index);
//            index--;
            cyclicBarrier.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws Exception {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(11, new Runnable() {
            public void run() {
                System.out.println("所有特工到达屏障，准备开始执行秘密任务");
            }
        });
        for (int i = 0; i < 10; i++) {
            new Thread(new CyclicBarrierTest(cyclicBarrier, i)).start();//拦截10个子线程
        }
        cyclicBarrier.await();//拦截main线程。满了11个。屏障打开
        System.out.println("全部到达屏障....");
    }

}
