package it.edu.demo.arraysum;

import it.edu.demo.utils.SumUtils;
import it.edu.demo.utils.Utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/**
* 2.多线程的方式,分段求和，然后汇总。
* 水平拆分任务。一个线程一个任务
* */
public class SumMultiThreads {
    public final static int NUM = 1000;

    public static class SumTask implements Callable<Long> {
        int lo;
        int hi;
        int[] arr;

        public SumTask(int[] a, int l, int h) {
            lo = l;
            hi = h;
            arr = a;
        }

        public Long call() { //override must have this type
            //System.out.printf("The range is [%d - %d]\n", lo, hi);
            long result = SumUtils.sumRange(arr, lo, hi);
            return result;
        }
    }

    public static long sum(int[] arr, ExecutorService executor) throws Exception {
        long result = 0;
        int numThreads = (arr.length / NUM > 0) ? (arr.length / NUM) : 1;

        SumTask[] tasks = new SumTask[numThreads];
        Future<Long>[] sums = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            tasks[i] = new SumTask(arr, (i * NUM), ((i + 1) * NUM)); //拆分任务
            sums[i] = executor.submit(tasks[i]);
        }

        for (int i = 0; i < numThreads; i++) {
            result += sums[i].get(); //每个子任务进行求和。
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        int[] arr = Utils.buildRandomIntArray(200_000);
        int numThreads = arr.length / NUM > 0 ? arr.length / NUM : 1;

        System.out.printf("The array length is: %d\n", arr.length);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        long result = sum(arr, executor);

        System.out.printf("The result is: %d\n", result);
    }
}
