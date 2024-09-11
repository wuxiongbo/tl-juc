package com.it.edu.map.Test;

import java.util.HashMap;

/**
 * @author     ：
 * @date       ：Created in 2019/5/26 16:24
 * @description：天下风云出我辈，一入代码岁月催
 * @version:  V1.0
 */
public class Test {

    static void run(int mapSize) { //初始化Map大小
        HashMap<Key, Integer> map = new HashMap<Key,Integer>(mapSize);
        for (int i = 0; i < mapSize; ++i) {
            map.put(Keys.hash(i), i);  //添加元素
        }

        long beginTime = System.nanoTime();

        for (int i = 0; i < mapSize; i++) {
            map.get(Keys.hash(i));    //取出元素
        }

        long endTime = System.nanoTime();

        System.out.println("getKey的平均时间:-->"+((endTime - beginTime)/mapSize));
    }

    //在hash均匀的情况下
    public static void main(String[] args) {
        for(int i=10; i<= 10000000; i*=10){ //开10000000个线程
            System.out.println("i=:"+i);
            run(i);
        }
    }

}
