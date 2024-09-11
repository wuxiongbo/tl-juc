package com.it.edu.jmm;

/**
 * volatile无法保证原子性
 * 创建十个线程，分别去对共享变量计数累加。查看结果。
 */
public class VolatileAtomicSample {

    private static /*volatile*/ int counter = 0;

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(()->{
                for (int j = 0; j < 1000; j++) {
                    /*
                    * 我们发现，即便加了volatile关键字，运行的结果依然是不确定的,
                    * 原因是, counter++ 不是一个原子操作。
                    * 这说明一个问题，
                    * volatile关键字，虽然可以解决可见性的问题。但是，无法保证原子性。
                    * 假设，线程1在第一轮循环结束后,
                    * 有其他线程已经将变量值改为M状态了(至于有没有回写到主内存，是存在不确定性的)
                    * 此时，线程1中的缓存行 就会被通知已失效，置为I状态，
                    * 导致计算结果没有从 `工作内存` 刷入 `主内存`，
                    * 相当于这一轮循环无效。
                    * 第二轮循环，线程1可能会读延迟（处于读等待状态），
                    * 直到变量值被其他线程刷回主内存，线程1才会重新去主存读取该变量
                    *
                    * 这里提到的 读延迟，CPU并不是傻傻的在那儿等数据回写，实际上它会继续执行后续的指令。
                    * 这里，就引申出了另外一个问题————指令重排
                    * 涉及到了，代码的执行顺序问题。
                    * */
                    counter++;
                    //1 load counter 到工作内存
                    //2 add counter 执行自加
                    /**
                     *
                     *其他的逻辑代码段
                     *
                     */
                }
            });
            thread.start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(counter);
    }

}
