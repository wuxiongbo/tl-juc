package it.edu.demo.arraysum;

import it.edu.demo.utils.Utils;

import java.util.concurrent.*;
/**
 *
 * 3.使用普通线程池的方式，引入分治思想
 *
 * 分而治之，线程卡死
 *
 * 任务分治思想，递归求和。
 * 任务的水平，垂直拆分。
 *
 * 本示例，计算不出结果，会一直卡死。
 * */
public class SumRecursiveMT {

    public static class RecursiveSumTask implements Callable<Long> {
        public static final int SEQUENTIAL_CUTOFF = 0;
        int lo;
        int hi;
        int[] arr; // arguments
        ExecutorService executorService;

        RecursiveSumTask( ExecutorService executorService, int[] a, int l, int h) {
            this.executorService = executorService;
            this.arr = a;
            this.lo = l;
            this.hi = h;
        }

        @Override
        public Long call() throws Exception { // override
            System.out.format("%s range [%d-%d] begin to compute %n",
                    Thread.currentThread().getName(), lo, hi);
            long result = 0;
            if (hi - lo <= SEQUENTIAL_CUTOFF) { //任务不可拆分
                for (int i = lo; i <= hi; i++) {
                    result += arr[i];
                }

                System.out.format("%s range [%d-%d] begin to finished %n",
                        Thread.currentThread().getName(), lo, hi);
            }
            else { //任务可拆分
                //递归提交任务。直到任务不可再拆分
                RecursiveSumTask left = new RecursiveSumTask(executorService, arr, lo, (hi - lo) / 2);
                RecursiveSumTask right = new RecursiveSumTask(executorService, arr, (hi - lo) / 2 + 1, hi);
                Future<Long> lr = executorService.submit(left);//提交成功即启动新线程
                Future<Long> rr = executorService.submit(right);//核心线程数满4个，任务提交到阻塞队列。
                // T4会阻塞在rr.get方法这里等待前面的任务执行完成。
                // 然而，不幸的是，前面四个线程全部都在一直等待子任务完成。 这将导致一直阻塞。
                result = lr.get() + rr.get();
                System.out.format("%s range [%d-%d] finished to compute %n",
                        Thread.currentThread().getName(), lo, hi);
            }

            return result;
        }
    }


    public static long sum(int[] arr) throws Exception {
//        int nofProcessors = Runtime.getRuntime().availableProcessors();
        //ExecutorService executorService = Executors.newCachedThreadPool();

        // 创建一个只有4个线程的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        //
        RecursiveSumTask task = new RecursiveSumTask(executorService, arr, 0, arr.length-1);

        long result =  executorService.submit(task).get();

        return result;
    }


    public static void main(String[] args) throws Exception {
        //返回  java虚拟机  中的内存总量
        long totalMemory = Runtime.getRuntime().totalMemory();
        //返回  java虚拟机  试图使用的最大内存量
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("Total_Memory(-Xms ) =  "+ totalMemory + " 字节  " + (totalMemory / (double)1024/1024)+"MB");
        System.out.println("Max_Memory(-Xmx ) =  "+ maxMemory + " 字节  " + (maxMemory / (double)1024/1024)+"MB");

        /*
         * Total_Memory(-Xms ) =  128974848 字节  123.0MB
         * Max_Memory(-Xmx ) =  1888485376 字节  1801.0MB
         *
         * */

        // 生成随机数组
//        int[] arr = Utils.buildRandomIntArray(3);//有结果
        int[] arr = Utils.buildRandomIntArray(5);//没有结果
//        int[] arr = Utils.buildRandomIntArray(100000000);//没有结果。会一直阻塞
        System.out.printf("The array length is: %d\n", arr.length);


        //**关键** 创造的线程池中只有4个线程
        long result = sum(arr);

        System.out.printf("The result is: %d\n", result);

    }

}