package it.edu.Atomic;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @author ：图灵-杨过
 * @date：2019/7/14
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : Atomic之 数组类型
 */
public class AtomicIntegerArrayTest {
    static int[] array = new int[]{1,2};

    //通过源码发现。这里通过构造方法创建的数组，是克隆了一份原来的数组
    static AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(array);


    public static void main(String[] args) {
        //将数组中下标为0的位置的值改为3。
        atomicIntegerArray.getAndSet(0,3);
        //获取数组中，下标为0的位置的值
        System.out.println("atomic:"+atomicIntegerArray.get(0));

        System.out.println("array:"+array[0]);

        //克隆数组 与 原数组 0索引位置的值明显不相等
        String result = atomicIntegerArray.get(0) == array[0] ? "相等":"不相等";
        System.out.println("是否相等？  "+result);
    }

}
