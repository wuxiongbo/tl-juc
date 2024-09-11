package com.it.edu.map;

//import com.it.edu.map.hashmap7.HashMap;
import com.it.edu.map.hashmap8.HashMap;
import com.it.edu.map.hashmap8.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author: 图灵学院-杨过
 * QQ：692927914
 * @date: 2019-04-14 21:28
 * @description: 往HashMap7中添加十万个元素。触发HashMap扩容
 *
 */
public class MapResizer implements Runnable {
    private static Map<Integer,Integer> map = new HashMap<>(2);
//    private static Map<String,String> map = new HashMap<>(2);

    private static AtomicInteger atomicInteger = new AtomicInteger();

    public void run() {
        while(atomicInteger.get() < 100000){
            map.put(atomicInteger.get(),atomicInteger.get());
//            map.put(String.valueOf(atomicInteger.get()),String.valueOf(atomicInteger.get()));
            atomicInteger.incrementAndGet();
            System.out.println(Thread.currentThread().getName()+"线程");
        }
    }
}
