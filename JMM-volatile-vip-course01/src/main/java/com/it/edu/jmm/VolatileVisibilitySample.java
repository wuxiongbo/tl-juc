package com.it.edu.jmm;

import java.util.concurrent.TimeUnit;

/**
 * @author ：图灵-杨过
 * @date：2019/6/25
 * @version: V1.0
 * @slogan:天下风云出我辈，一入代码岁月催
 *
 * volatile的可见性
 */
public class VolatileVisibilitySample {
    private boolean initFlag = false;
    static Object object = new Object();

    public void refresh() {
        this.initFlag = true; //普通写操作，(volatile写)
        String threadname = Thread.currentThread().getName();
        System.out.println("线程：" + threadname + ":修改共享变量initFlag");
    }

    //自加
    public void load() {
        String threadname = Thread.currentThread().getName();
        int i = 0;
        while (!initFlag) {
            //加了synchronized关键字后，就有可能产生线程的竞争与阻塞，从而导致线程的上下文切换。
            synchronized (object) {
                i++;
            }
            //i++;
        }
        System.out.println("线程：" + threadname + "当前线程嗅探到initFlag的状态的改变" + i);
    }

    //空循环
   /* public void load(){
        String threadname = Thread.currentThread().getName();
        while (!initFlag){
            //threadA修改了initFlag后， threadB感知不到，会一直空循环
            //空循环可以理解为一个自旋，拥有非常高的CPU使用权限，别的线程就抢不到该线程的资源。
        }
        System.out.println("线程："+threadname+"当前线程嗅探到initFlag的状态的改变"+i);
    }*/

    public static void main(String[] args) {
        VolatileVisibilitySample sample = new VolatileVisibilitySample();
        Thread threadA = new Thread(() -> {
            sample.refresh();
        }, "threadA");

        Thread threadB = new Thread(() -> {
            sample.load();
        }, "threadB");

        threadB.start();
        try {
            Thread.sleep(2000); //main线程休眠，threadB线程正常执行。
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        threadA.start();
    }

}
