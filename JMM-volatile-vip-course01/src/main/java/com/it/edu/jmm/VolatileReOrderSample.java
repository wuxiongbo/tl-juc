package com.it.edu.jmm;

import com.it.edu.util.UnsafeInstance;

/**
 * @author ：
 * @date ：Created in 2019/6/24 21:36
 * @description：天下风云出我辈，一入代码岁月催
 *
 * 并发场景下存在指令重排
 */
public class VolatileReOrderSample {
    private static int x = 0, y = 0;
    private /*volatile*/ static int a = 0, b = 0;  //使用volatile内存语义 解决指令重排问题

    public static void main(String[] args) throws InterruptedException {
        int i = 0;

        for (;;) { //死循环重复以下逻辑，以确保复现指令重排的问题。
            i++;
            x = 0;
            y = 0;
            a = 0;
            b = 0;
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    //由于 线程1 先启动，所以，这里使用下面这句话，让它等一等 线程2.
                    //读者可根据自己电脑的实际性能适当调整等待时间.
                    shortWait(10000);

                    a = 1; //问：这一步是读还是写呢？ 答： 写。volatile写

                    /*
                    使用volatile关键字修饰后，这里，加了个 StoreLoad读写屏障，
                    从而避免CPU对其进行指令重排。
                    这里是 不允许 第一步的volatile写(对变量a赋值) 与 第二步volatile读(读变量b的值) 发生重排
                    */

                    //也可手动加内存屏障。进入Unsafe类查找带Fence关键字的方法。
                    //然后，使用Unsafe类的storeFence方法。
                    // 此方法，会将`读写`屏障一起插入
//                    UnsafeInstance.reflectGetUnsafe().storeFence();

                    x = b; //问：这一步是读还是写呢？  答：读写都有。先读volatile变量，再写入普通变量
                    //这里实际拆解为两步进行：第一步 先执行 volatile读。第二步 再执行 普通写
                }
            });
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    b = 1;

//                    UnsafeInstance.reflectGetUnsafe().storeFence();

                    y = a;
                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();

            /**
             * 不加volatile关键字修饰 a、b变量，运行此程序。
             * x,y 的值会出现以下组合
             * 1,1  T1、T2执行过程中， a,b均赋值完成
             * 0,1  T2先执行，b赋值完成
             * 1,0  T1先执行，a赋值完成
             *
             * 然而，实际上，还出现了下面的结果
             * 0,0
             *
             * 按照常规的逻辑来分析，是不会出现 0,0这种情况的。
             * 除非代码执行顺序发生了变化，
             *   出现 x = b; y = a; 均先于 a = 1; b = 1; 执行，才会发生这种情况
             *
             * 事实上，的确是因为发生了代码执行顺序的变化，而产生了这种结果
             * 原因是，cpu或者jit对我们的代码进行了指令重排。
             *
             * 我们可以使用内存屏障解决指令重排的问题。
             */
            String result = "第" + i + "次 (" + x + "," + y + "）";
            if (x == 0 && y == 0) {
                System.err.println(result);
                break;
            } else {
                System.out.println(result);
            }
        }

    }

    public static void shortWait(long interval) {
        long start = System.nanoTime();
        long end;
        do {
            end = System.nanoTime();
        } while (start + interval >= end);
    }

}
