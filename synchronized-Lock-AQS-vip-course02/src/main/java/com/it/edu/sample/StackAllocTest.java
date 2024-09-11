package com.it.edu.sample;

/**
 * @author ：图灵-杨过
 * @date：2019/7/26
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : 逃逸分析
 */
public class StackAllocTest {

    /**
     * 进行两种测试
     * 关闭逃逸分析，同时调大堆空间，避免堆内GC的发生，如果有GC信息将会被打印出来
     * VM运行参数：-Xmx4G -Xms4G -XX:-DoEscapeAnalysis -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError
     *
     * 开启逃逸分析
     * VM运行参数：-Xmx4G -Xms4G -XX:+DoEscapeAnalysis -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError
     *
     * 执行main方法后
     * jps 查看进程
     * jmap -histo 进程ID
     *
     */

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 500000; i++) {
            alloc();
        }
        long end = System.currentTimeMillis();

        //查看执行时间
        costTime(start,end);


    }

    //将student return之后，jvm就不会对线程进行逃逸优化。
    // 因为方法中创建的对象有可能被其他线程引用到。
    //是否可能被其他线程引用到，是判断的关键
/*    private static TulingStudent alloc() {
        TulingStudent student = new TulingStudent();
        return student;
    }*/

   //此方法没有返回值，JIT在编译时会对代码进行 逃逸分析
    private static void alloc() {
        //逃逸分析，优化后，不是所有对象都存放在堆区，还有一部分存在线程栈空间
        TulingStudent student = new TulingStudent();
    }



    static void costTime(long start,long end){
        System.out.println("cost-time " + (end - start) + " ms");
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}

class TulingStudent {
    private String name;
    private int age;
}