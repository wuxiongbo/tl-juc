package it.edu.demo.utils;

import java.util.Random;

/**
 * @author     ：杨过
 * @date       ：Created in 2019/7/30 14:23
 * @description：天下风云出我辈，一入代码岁月催
 * @modified By：
 * @version:  V1.0
 */
public class Utils {
    /**
     * 创建固定长度的随机数组
     * @param size
     * @return
     */
    public static int[] buildRandomIntArray(int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Random().nextInt(100);
        }
        return array;
    }

    /**
     * 创建随机数组
     * */
    public static int[] buildRandomIntArray() {
        int size = new Random().nextInt(100);
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = new Random().nextInt(100);
        }
        return array;
    }

}