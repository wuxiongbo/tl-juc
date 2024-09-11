package com.it.edu.sample;

/**
 * @author ：图灵-杨过
 * @date：2019/7/26
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description :
 */
public class SynchronizedTest {
    StringBuffer stb = new StringBuffer();

    /**
     * ==锁的粗化==
     *
     * java.lang.StringBuffer#append(java.lang.String)
     *
     *     @Override
     *     public synchronized StringBuffer append(String str) {
     *         toStringCache = null;
     *         super.append(str);
     *         return this;
     *     }
     *
     *  可以看到 StringBuffer的append方法是线程安全的。
     *  按照一般逻辑思维来讲，这里每调用一次append，都会加一个synchronized同步块，
     *  这意味着多线程并发访问的时候，会频繁的加锁解锁，从而导致频繁的上下文切换
     *
     *  实际上，jvm 会智能的对其进行优化，对多次调用的append方法，JVM只进行一次全局的加锁解锁。
     *  这个过程，就叫做锁的粗化。
     *
     */
    public void test1(){
        //jvm的优化之锁的粗化
        stb.append("1");

        stb.append("2");

        stb.append("3");

        stb.append("4");
    }


    /**
     * ==锁的消除==
     *
     * jvm的优化，即，JVM不会对同步块进行加锁
     * jvm会对锁进行逃逸分析，即，多个线程是否会访问到
     *
     * 分析思路：
     *   T1线程，访问test2方法，它会有个自己的new Object()
     *   T2线程，访问test2方法，也会有个自己的new Object()
     *   T1线程与T2线程中的Object 并不是同一个Object，所以，多个线程，相互之间访问不到对方的Object。
     *   T1在Object中加锁，T2根本看不到。
     */
    public void test2(){
        synchronized (new Object()) {
            //伪代码：我们假设这里有很多逻辑
            //问：这里的锁每次都是new的实例对象，那么jvm是否会对这里进行加锁呢？
            //答：jvm会进行 逃逸分析， 并进行相应的优化。
        }
    }

    /**
     * add()方法中的局部对象sBuf，就只在该方法内的作用域有效，
     * 不同线程同时调用 add()方法时，都会创建不同的sBuf对象，
     * 因此，此时的append操作若是使用同步操作，就是白白浪费的系统资源。
     *
     * 这时我们可以通过编译器将其优化，将锁消除，
     * 前提是 1)java必须运行在server模式（server模式会比client模式作更多的优化），
     *       2)同时必须开启逃逸分析:
     *
     * -server -XX:+DoEscapeAnalysis -XX:+EliminateLocks
     *
     * 其中 +DoEscapeAnalysis 表示开启 逃逸分析，
     *     +EliminateLocks   表示开启 锁消除。
     * @param str1
     * @param str2
     * @return
     */
    public String add(String str1,String str2){
        StringBuffer sBuf = new StringBuffer();
        sBuf.append(str1);// append方法是同步操作
        sBuf.append(str2);
        return sBuf.toString();
    }

    public static void main(String[] args) {
        SynchronizedTest test = new SynchronizedTest();

        // 锁的消除
        test.test2();

        // 锁的粗化
        test.test1();

        // 逃逸分析 + 锁消除
        test.add("1","2");

    }
}
