package it.edu.demo.countedcompleter;

import it.edu.demo.countedcompleter.completer.FactorialTask;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
/**
 * 入门示例：阶乘
 *
 * ForkJoinTask 的子类 CountedCompleter： 在任务完成执行后会触发执行一个自定义的钩子函数
 *
 * 对比 之前的示例，SumRecursiveMT 类。
 * */
public class CountedCompleterExample {

    public static void main (String[] args) {
        List<BigInteger> list = new ArrayList<>();
        for (int i = 3; i < 20; i++) {
            list.add(new BigInteger(Integer.toString(i)));
        }

        ForkJoinPool.commonPool().invoke(new FactorialTask(null, list));
    }


}