package com.it.edu.queue;

import com.it.edu.queue.util.ArrayBlockingQueue;
import com.it.edu.queue.bean.Ball;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @description: 条件队列的应用： ArrayBlockingQueue 有界阻塞队列
 *               自定义一个箱子，演示并分析条件队列
 * @author: 图灵学院-杨过
 * QQ：692927914
 */
public class ArrayBlockingQueueTest {
    /**
     * 创建容量大小为1的 ‘有界’队列。 该容量参数会作为条件队列中的判断依据
     */
    private BlockingQueue<Ball> blockingQueue = new ArrayBlockingQueue<Ball>(1);

    /**
     * 队列大小
     * @return
     */
    public int queueSize(){
        return blockingQueue.size();
    }

    /**
     * 将球放入队列当中,生产者
     * @param ball
     * @throws InterruptedException
     */
    public void produce(Ball ball) throws InterruptedException{
        blockingQueue.put(ball);
    }

    /**
     * 将球从队列当中拿出去，消费者
     * @return
     */
    public Ball consume() throws InterruptedException {
       return blockingQueue.take();
    }


    static boolean hasDate = false;//默认没数据
    public static void main(String[] args){
        final ArrayBlockingQueueTest box = new ArrayBlockingQueueTest();
        ExecutorService executorService = Executors.newCachedThreadPool(); //线程池

        final Ball lock = new Ball();

        /**
         * producer,该线程 不断往箱子里面放入乒乓球(生产)
         */
        executorService.submit(() -> {
            int i = 0;
            while (true){
                try {
                    synchronized (lock){
                        if(hasDate){
                            lock.wait();//有数据，阻塞当前生产线程，等待消费
                        }

                        Ball ball = new Ball();
                        ball.setNumber("乒乓球编号:"+i);
                        ball.setColor("yellow");

                        System.out.println("p时间戳 "+System.currentTimeMillis()+
                                ",准备往箱子里放入乒乓球:--->new"+ball.getNumber());

                        box.produce(ball); //**关键** put

                        System.out.println("p时间戳 "+System.currentTimeMillis()+
                                ",往箱子里放入乒乓球:--->p"+ball.getNumber());
                        System.out.println("put操作后，当前箱子中共有乒乓球:--->"
                                + box.queueSize() + "个");
                        System.out.println("---------------------");

                        hasDate = true;//标记为有数据

                        Thread.sleep(1000);
                        lock.notify();//put完毕，唤醒消费线程
                        i++; //更新 乒乓球编号。
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        /**
         * consumer, 该线程 不断从箱子里面拿球出来(消费)
         */
        executorService.submit(() -> {
            while (true){
                try {
                    synchronized (lock){
                        if(!hasDate){
                            lock.wait();//没有数据，阻塞当前消费线程，等待生产
                        }

                        System.out.println("t时间戳 " + System.currentTimeMillis() +
                                ",准备到箱子中拿乒乓球");
                        Ball ball = box.consume();//**关键** take
                        System.out.println("t时间戳 " + System.currentTimeMillis() +
                                ",拿到箱子中的乒乓球:--->t" + ball.getNumber());
                        System.out.println("take操作后，当前箱子中共有乒乓球:--->"
                                + box.queueSize() + "个");
                        System.out.println("=====================");

                        hasDate = false;//标记为没数据
                        Thread.sleep(1000);
                        lock.notify();//take完毕，唤醒生产线程
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
