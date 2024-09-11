package com.it.edu.map;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：图灵-杨过
 * @date：2019/7/17
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : JDK1.8 HashMap 存在数据丢失
 */
public class HashMapDataLost {
    public static final Map<String, String> map = new HashMap<String, String>();

    public static void main(String[] args) {
        //线程一
        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                map.put(String.valueOf(i), String.valueOf(i));
            }
        }).start();

        //线程二
        new Thread(() -> {
            for (int j = 1000; j < 2000; j++) {
                map.put(String.valueOf(j), String.valueOf(j));
            }
        }).start();

        //主线程休眠等待 线程一、线程二 完成任务
        try {
            System.out.println(Thread.currentThread().getName()+"线程等待");
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("map.size: "+map.size());

        //输出
        for(int i=0;i<2000;i++){
            System.out.println("第："+i+"元素，值：" + map.get(String.valueOf(i)));
        }

        //result
//        第：0元素，值：0
//        第：1元素，值：1
//        第：2元素，值：null
//        第：3元素，值：3
//        第：4元素，值：null
//        第：5元素，值：5
//        第：6元素，值：null
//        第：7元素，值：7
//        第：8元素，值：null
//        JDK1.8 HashMap 在并发环境下存在数据丢失的问题。
    }
}
