package com.it.edu.jmm;

/**
 * @author ：图灵-杨过
 * @date：2019/7/10
 * @version: V1.0
 * @slogan:天下风云出我辈，一入代码岁月催
 * @description  通过单例模式，简要说明 指令重排现象。
 */
public class Singleton {

    /**
     * 查看汇编指令
     * -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -Xcomp
     */
    private volatile static Singleton myinstance;

    public static Singleton getInstance() {
        if (myinstance == null) {
            synchronized (Singleton.class) {
                if (myinstance == null) {
                    myinstance = new Singleton();//对象创建过程，本质上可以分为三步。
                                              // 1.分配对象内存空间；
                                              // 2.初始化对象；
                                              // 3.指针指向刚分配的内存空间地址；
                    //对象延迟初始化
                    //
                }
            }
        }
        return myinstance;
    }

    public static void main(String[] args) {
        Singleton.getInstance();
    }

}
