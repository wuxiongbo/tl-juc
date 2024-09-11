package it.edu.demo.countedcompleter.completer;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 搜索任务
 * @param <E>
 */
public class SearcherTask<E> extends CountedCompleter<E> {
    private final E[] items;
    private final E expected;
    private final AtomicReference<E> result;
    private final int startIndex, endIndex;
    private final Configuration configuration;

    public SearcherTask(CountedCompleter<?> completer, E[] items, AtomicReference<E> result, int startIndex, int endIndex, E expected, Configuration configuration) {
        super(completer);
        this.items = items;
        this.result = result;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.expected = expected;
        this.configuration = configuration;
    }

    @Override
    public E getRawResult() {
        return result.get();
    }

    @Override
    public void compute() {
        int localStartIndex = startIndex,  localEndIndex = endIndex;
        while (result.get() == null && localEndIndex >= localStartIndex) {
            /**
             * If there are more than one element to check, we divide it until receiving
             * the single one item.
             */
            if (localEndIndex - localStartIndex >= 2) {
                int mid = (localStartIndex + localEndIndex) >>> 1;
                /**
                 * Disable this method invocation to see that the search won't work even
                 * if searched item exists in the array.
                 * Imagine the situation when you search a "J" letter in alphabet array "A"-"J".
                 * CountedCompleter will first split this array to 2 parts, after split them until
                 * receiving 1-elements array. So if the tasks subset without "J" ends before the
                 * subset containing "J", the result will be null.
                 */
                if (!configuration.canSkipIncrement()) {
                    addToPendingCount(1);
                }
                new SearcherTask(this, items, result, mid, localEndIndex, expected, configuration).fork();
                localEndIndex = mid;
            } else {
                /**
                 * We have now only one item, so we can check if it matches searched element and
                 * if it's different from null. If the item fulfils both conditions, we quit the execution.
                 * We do so by calling quietlyCompleteRoot(). This method completes task normally without
                 * setting new value (see setRawResult() method in CountedCompleter abstract class, which is btw
                 * empty by default).
                 *
                 * This matching value (null in our case because we work with AtomicReference) will be
                 * returned for all related operations.
                 *
                 * Without calling quietlyCompleteRoot(), we consider that the result was not found. In consequence,
                 * program will continue the execution. To observe that, simply comment below invocation. To make
                 * it working, we explicitly set the number of pending tasks to 333 in test declaration (much more
                 * than real tasks to execute). Note that quietlyCompleteRoot() can be also declared with
                 * complete(T result) method call when one of subtasks generated the expected result.
                 */
                E item = items[localStartIndex];
                if (matches(item) && result.compareAndSet(null, item)) {
                    if (!configuration.canSkipCompleteCall()) {
                        quietlyCompleteRoot();
                    }
                }
                break;
            }
        }
        /**
         *
         * When tryComplete() is not called, we still consider that the task is not finished. In our case it's visible
         * only for the case when searched letter doesn't exist in submitted array.
         */
        if (!configuration.canSkipTryCompleteCall()) {
            tryComplete();
        }
    }

    boolean matches(E e) {
        return e.equals(expected);
    }

    @Override
    public String toString() {
        return "SearcherTask {"+startIndex+"-"+endIndex+"}";
    }
}
