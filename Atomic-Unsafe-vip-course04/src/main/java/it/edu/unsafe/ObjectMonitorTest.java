package it.edu.unsafe;

import it.edu.utils.UnsafeInstance;
import sun.misc.Unsafe;

/**
 * @author ：图灵-杨过
 * @date：2019/8/2
 * @version: V1.0
 * @slogan: 天下风云出我辈，一入代码岁月催
 * @description : unsafe之 对象锁
 */
public class ObjectMonitorTest {
    static Object object = new Object();

/*    public void method1(){
        unsafe.monitorEnter(object);
    }

    public void method2(){
        unsafe.monitorExit(object);
    }*/

    public static void main(String[] args) {

        /*synchronized (object){
        }*/
        Unsafe unsafe = UnsafeInstance.reflectGetUnsafe();

        unsafe.monitorEnter(object);  //加锁
        //业务逻辑写在此处之间
        unsafe.monitorExit(object);  //解锁

    }

}
