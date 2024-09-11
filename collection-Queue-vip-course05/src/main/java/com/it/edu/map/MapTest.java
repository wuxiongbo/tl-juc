package com.it.edu.map;

/**
 * @author ：图灵-杨过
 * @date：2019/7/17
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : jdk1.7 HashMap 存在死循环问题
 */
public class MapTest {

    public static void main(String[] args) {

        for (int i=0;i<30;i++){
            new Thread(new MapResizer()).start();
            System.out.println(Thread.currentThread().getName()+"线程"+i);
        }

    }
}
