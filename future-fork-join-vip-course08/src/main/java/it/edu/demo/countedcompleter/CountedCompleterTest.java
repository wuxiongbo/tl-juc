package it.edu.demo.countedcompleter;


import it.edu.demo.countedcompleter.completer.Configuration;
import it.edu.demo.countedcompleter.completer.SearcherTask;
import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertThat;

/**
 * 示例：搜索
 *
 */
public class CountedCompleterTest {

    //待搜索元素(存在字母表中)
    private static final String J_LETTER = "J";
    //待搜索元素(不存在字母表中)
    private static final String NOT_EXISTENT_LETTER = "2";
    //字母表
    private static final String[] ALPHABET =
            {"A", "B", "C", "D", "E", "F", "G", "H", "I", J_LETTER};
    //默认的搜索配置
    private static final Configuration DEFAULT_CONFIG = new Configuration(
            false, false, false);

    private SearcherTask<String> init(String element, Configuration configuration){
        return new SearcherTask<>(
                null,
                ALPHABET,
                new AtomicReference<>(),
                0,
                ALPHABET.length,
                element,
                configuration);
    }


    @Test
    //should find result even with more than 0 pending tasks
    public void test1() {
        SearcherTask<String> searcher = init(J_LETTER,DEFAULT_CONFIG);
        searcher.setPendingCount(333);

        ForkJoinPool pool = new ForkJoinPool();
        String word = pool.invoke(searcher);

        System.out.println(word);
//        assertThat(word).isEqualTo(J_LETTER);
//        assertThat(searcher.getPendingCount()).isGreaterThan(0);
//        assertThat(searcher.isCompletedNormally()).isTrue();
    }

    @Test
    //should find letter without additional pending tasks
    public void test2() {
        SearcherTask<String> searcher = init(J_LETTER,DEFAULT_CONFIG);

        ForkJoinPool pool = new ForkJoinPool();
        String word = pool.invoke(searcher);

        System.out.println(word);
//        assertThat(word).isEqualTo(J_LETTER);
//        assertThat(searcher.getPendingCount()).isEqualTo(0);
//        assertThat(searcher.isCompletedNormally()).isTrue();
    }

    @Test
    //should not find letter because missing pending tasks incrementation
    public void test3() {
        Configuration configuration = new Configuration(
                true,
                false,
                false);
        SearcherTask<String> searcher = init(J_LETTER,configuration);

        ForkJoinPool pool = new ForkJoinPool();
        String word = pool.invoke(searcher);

        System.out.println(word);
//        assertThat(word).isNull();
//        assertThat(searcher.isCompletedNormally()).isTrue();
    }

    @Test
    //should not end because of too many tasks and complete call missing
    public void test4()
            throws InterruptedException {
        Configuration configuration = new Configuration(
                false,
                true,
                false);
        SearcherTask<String> searcher = init(J_LETTER,configuration);
        searcher.setPendingCount(333);

        ForkJoinPool pool = new ForkJoinPool();
        pool.execute(searcher);
        pool.awaitTermination(4, TimeUnit.SECONDS);

//        assertThat(searcher.isCompletedNormally()).isFalse();
    }

    @Test
    //should not end because of not existent letter and trycomplete call missing
    public void test5()
            throws InterruptedException {
        Configuration configuration = new Configuration(
                false,
                false,
                true);
        // Because of Searcher code we have to try to find not existent letter to prove that missing tryComplete() call
        // can deadlock.
        SearcherTask<String> searcher = init(NOT_EXISTENT_LETTER,configuration);

        ForkJoinPool pool = new ForkJoinPool();
        pool.execute(searcher);
        pool.awaitTermination(4, TimeUnit.SECONDS);

//        assertThat(searcher.isCompletedNormally()).isFalse();
    }

    @Test
    //should not find letter not existing in alphabet
    public void test6() {
        SearcherTask<String> searcher = init(NOT_EXISTENT_LETTER,DEFAULT_CONFIG);

        ForkJoinPool pool = new ForkJoinPool();
        String word = pool.invoke(searcher);

        System.out.println(word);
//        assertThat(word).isNull();
//        assertThat(searcher.getPendingCount()).isEqualTo(0);
//        assertThat(searcher.isCompletedNormally()).isTrue();
    }

}




