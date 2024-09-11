package it.edu.demo.arraysum;


import it.edu.demo.utils.SumUtils;
import it.edu.demo.utils.Utils;
/**
 *
* 1.普通的求和方式，顺序叠加求和
* */
public class SumSequential {

    public static long sum(int[] arr){
        return SumUtils.sumRange(arr, 0, arr.length);
    }

    public static void main(String[] args) {
        int[] arr = Utils.buildRandomIntArray();//创建一个长度不定的随机数组。
        System.out.printf("The array length is: %d\n", arr.length);

        long result = sum(arr);

        System.out.printf("The result is: %d\n", result);
    }
}
