package com.it.edu.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author ：图灵-杨过
 * @date：2019/7/14
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description :
 */
public class UnsafeInstance {
    /*
    * Unsafe 类中，定义了很多 可以越过虚拟机直接操作底层  的方法。
    * 因为直接越过虚拟机进行操作，是非常不安全的，所以此类只能通过Bootstrap 引导类加载器，去加载。
    * 所以，这里，只能通过反射的方式使用 Unsafe类。
    * 此方法做的事情，顾名思义，通过反射获取Unsafe类
    * */
    public static Unsafe reflectGetUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
