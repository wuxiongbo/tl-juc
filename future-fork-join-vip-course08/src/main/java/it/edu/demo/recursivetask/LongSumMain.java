package it.edu.demo.recursivetask;

import it.edu.demo.utils.Utils;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
/**
 * 数据求和
 * ForkJoinTask 的子类 RecursiveTask ：用于有返回结果的任务
 * 线性算法 与 ForkJoin 对比
* */
public class LongSumMain {
	//获取逻辑处理器数量
	static final int NCPU = Runtime.getRuntime().availableProcessors();
	/** for time conversion */
	static final long NPS = (1000L * 1000 * 1000);

	static long calcSum;

	static final boolean reportSteals = true;

    static long seqSum(int[] array) {
        long sum = 0;
        for (int i = 0; i < array.length; ++i)
            sum += array[i];
        return sum;
    }


	public static void main(String[] args) throws Exception {
		int[] array = Utils.buildRandomIntArray(20_000_000);
		System.out.println("cpu-num:"+NCPU);

		//演示一：单线程下计算数组数据总和
 		calcSum = seqSum(array);
		System.out.println("seq sum=" + calcSum);

		//演示二：采用fork/join方式将数组求和任务进行拆分执行，最后合并结果
		LongSum ls = new LongSum(array, 0, array.length);
  		ForkJoinPool fjp  = new ForkJoinPool(NCPU); //使用的线程数
		ForkJoinTask<Long> task = fjp.submit(ls);
		System.out.println("forkjoin sum=" + task.get());
		/* ForkJoinTask 在执行的时候可能会抛出异常，但是我们没办法在主线程里直接捕获异常，
		   所以，ForkJoinTask 提供了 isCompletedAbnormally()方法
		   来检查任务是否已经抛出异常 或 已经被取消了，
		   并且，可以通过 ForkJoinTask 的getException()方法  获取异常。*/
		if(task.isCompletedAbnormally()){
			System.out.println(task.getException());
		}
		fjp.shutdown();

	}

}