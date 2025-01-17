package it.edu.Atomic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：图灵-杨过
 * @date：2019/8/2
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : CAS操作的ABA问题。
 */
public class AtomicAbaProblemTest {

    static AtomicInteger atomicInteger = new AtomicInteger(1);

    public static void main(String[] args) {
        Thread main = new Thread(new Runnable() {
            @Override
            public void run() {
                int value = atomicInteger.get();
                System.out.println("操作线程"+Thread.currentThread().getName()+"--修改前操作数值:"+value);

                try {
                    Thread.sleep(1000); //当前线程休眠期间，其他线程偷偷修改数据
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                boolean isCasSuccess = atomicInteger.compareAndSet(value,2);

                if(isCasSuccess){
                    System.out.println("操作线程"+Thread.currentThread().getName()+"--Cas修改后操作数值:"+atomicInteger.get());
                }else{
                    System.out.println("CAS修改失败");
                }

            }
        },"主线程");

        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();// 1+1 = 2;
                System.out.println("操作线程"+Thread.currentThread().getName()+"--increase后值:"+atomicInteger.get());
                atomicInteger.decrementAndGet();// atomic-1 = 2-1;
                System.out.println("操作线程"+Thread.currentThread().getName()+"--decrease后值:"+atomicInteger.get());
            }
        },"干扰线程");

        main.start();  //主线程
        other.start(); //干扰线程
    }
}
