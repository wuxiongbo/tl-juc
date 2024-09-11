package it.edu.demo.countedcompleter.completer;

import it.edu.demo.utils.CalcUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CountedCompleter;

/**
 * 阶乘任务
 */
public class FactorialTask extends CountedCompleter<Void> {

    private static int SEQUENTIAL_THRESHOLD = 5;
    private List<BigInteger> integerList;
    private int numberCalculated;

    public FactorialTask (CountedCompleter<Void> parent,
                          List<BigInteger> integerList) {
        super(parent);
        this.integerList = integerList;
    }

    @Override
    public void compute () {
        if (integerList.size() <= SEQUENTIAL_THRESHOLD) {
            showFactorial();
        } else {
            int middle = integerList.size() / 2;
            List<BigInteger> rightList = integerList.subList(middle,
                    integerList.size());
            List<BigInteger> leftList = integerList.subList(0, middle);
            addToPendingCount(2);
            FactorialTask taskRight = new FactorialTask(this, rightList);
            FactorialTask taskLeft = new FactorialTask(this, leftList);
            taskLeft.fork();
            taskRight.fork();
        }
        tryComplete(); //CountedCompleter的方法。
    }

    @Override
    //钩子函数
    public void onCompletion (CountedCompleter<?> caller) {
        if (caller == this) {
            System.out.printf("completed thread : %s numberCalculated=%s%n", Thread
                    .currentThread().getName(), numberCalculated);
        }
    }

    private void showFactorial () {

        for (BigInteger i : integerList) {
            BigInteger factorial = CalcUtil.calculateFactorial(i);
            System.out.printf("%s! = %s, thread = %s%n", i, factorial, Thread
                    .currentThread().getName());
            numberCalculated++;
        }
    }
}
