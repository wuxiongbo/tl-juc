package com.it.edu.aqs;

import com.it.edu.util.UnsafeInstance;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * @author ：图灵-杨过
 * @date：2019/7/3
 * @version: V1.0
 * @slogan:天下风云出我辈，一入代码岁月催
 * description：AQS同步器框架源码
 */
public abstract class AbstractQueuedSynchronizer
        extends AbstractOwnableSynchronizer
        implements java.io.Serializable {
    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     */
    protected AbstractQueuedSynchronizer() { }

    /**
     * Wait queue node class.
     *
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    static final class Node {
        /**
         * 标记结点为 共享模式
         * */
        static final Node SHARED = new Node();
        /**
         * 标记结点为 独占模式
         */
        static final Node EXCLUSIVE = null;

        /**
         * 在同步队列中等待的线程等待超时或者被中断，需要从同步队列中取消等待
         * */
        static final int CANCELLED =  1;
        /**
         *  后继结点的线程处于等待状态，而当前结点如果释放了同步状态或者被取消，
         *  将会通知后继结点，使后继结点的线程得以运行。
         */
        static final int SIGNAL    = -1;
        /**
         *  结点在等待队列中，结点的线程等待在Condition上，当其他线程对Condition调用了signal()方法后，
         *  该结点会从等待队列中转移到同步队列中，加入到同步状态的获取中
         */
        static final int CONDITION = -2;
        /**
         * 表示下一次共享式同步状态获取将会被无条件地传播下去
         */
        static final int PROPAGATE = -3;

        /**
         * 标记当前结点的信号量状态 (1,0,-1,-2,-3)5种状态
         * 使用CAS操作更改状态值，volatile修饰变量保证线程的可见性，
         * 使之在高并发场景下，被一个线程修改后，状态会立马让其他线程可见。
         */
        volatile int waitStatus;

        /**
         * 前驱结点。该字段在 将当前结点加入到同步队列中时 被设置
         */
        volatile Node prev;

        /**
         * 后继结点
         */
        volatile Node next;

        /**
         * 结点同步状态的线程
         */
        volatile Thread thread;

        /**
         * 该字段有两个作用：
         * 1.记录‘条件等待队列’中的后继结点，
         * 2.记录‘同步等待队列’中当前结点的资源共享模式。
         *   如果当前结点是共享的，那么这个字段是SHARED常量，
         *   如果当前结点是独占的，那么这个字段是EXCLUSIVE常量，
         *
         * 也就是说 ‘结点类型(独占/共享)’ 和 ‘条件等待队列中的后继结点’ 共用同一个字段。
         */
        Node nextWaiter;

        /**
         * Returns true if node is waiting in shared mode.
         */
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 返回前驱结点
         */
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }
        //无参构造方法，用来初始化结点
        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 指向同步等待队列的头结点
     */
    private transient volatile Node head;

    /**
     * 指向同步等待队列的尾结点
     */
    private transient volatile Node tail;

    /**
     * 同步资源状态
     */
    // 锁状态标志位(锁计数器)： volatile 修饰（为了实现多线程之间能通过此变量判断锁的状态，我们用volatile修饰）
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 结点加入CLH同步队列
     */
    private Node enq(final Node node) {
        // 经典的 lockfree 算法：循环+CAS
        // CAS"自旋"，直到成功加入队尾。
        for (;;) {
            Node t = tail; //拿到尾结点
            if (t == null) {//队列未初始化(尾结点指针为空，进入该方法)
                //尾结点为空，说明队列为空。
                //此时，必须先初始化头结点。即，创建一个空的标志结点作为head结点。
                //然后，CAS 修改头部结点的指向。
                if (compareAndSetHead(new Node()))//CAS初始化头结点head指针的位置。AQS搜索关键字2
                    //进行到这一行，队列初始化一半。此刻，head指向初始化结点，tail指向null;
                    tail = head;//将tail指向刚初始化的head结点。(队列只有一个初始化结点，因此，它是头也是尾。)
            } else { //队列已初始化(将当前结点放入队尾)
                node.prev = t;//将当前结点的前驱结点，指向旧的尾结点t。
                if (compareAndSetTail(t, node)) {//CAS更新尾指针tail的位置
                    t.next = node; //前驱结点t的next指针指向当前结点
                    return t; //返回当前结点的前驱结点
                }
            }
        }
    }

    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    private Node addWaiter(Node mode) {
        // 1. 将当前线程构建成mode类型的结点。mode有两种：EXCLUSIVE（独占）和SHARED（共享）
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 2.尝试将当前结点，以快速的方式，直接放到队尾。
        Node pred = tail; //首先，拿到当前同步等待队列的尾结点
        // 2.1 判断当前队列的尾结点是否为null。
        if (pred != null) {//如果这里 pred 为null，则走enq方法，对队列进行初始化。
            // 2.2 将当前结点的‘前驱指针’，指向尾结点。(将当前结点放入队尾)
            node.prev = pred;
            // 2.3 使用CAS原子操作，将前驱结点的‘尾指针’，指向当前结点。即，将结点插入同步队列的尾部
            //     如果CAS原子操作失败，说明存在线程竞争
            if (compareAndSetTail(pred, node)) {
                pred.next = node; //将‘前驱结点’的‘后继结点’指针，指向当前结点(实现双向链表数据结构)
                return node;
            }
        }
        //上一步失败(两种情况，1.队列未初始化，2.存在线程竞争)，则通过enq入队。
        //以lockfree的方式插入队尾(自旋)
        enq(node);
        return node;
    }

    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    private void setHead(Node node) {
        head = node;
        node.thread = null;  //获取到锁的线程 出队(取消绑定)
        node.prev = null;    //双向指针变成了单向指针(移除旧头结点)
    }

    /**
     * 从 release 进入到这里时，Node node是头结点
     */
    private void unparkSuccessor(Node node) {
        //获取 node的 wait信号量
        int ws = node.waitStatus;
        if (ws < 0) //状态为-1。
            // Semaphore中。T1唤醒T3，走到这里，可能T2将头结点waitStatus改为了-3。
            // ws为0、waitStatus为-3
            // 或 (ws为-3、waitStatus为-3)
            // 或 (ws为0、waitStatus为0 后续还是会置为-3)
            compareAndSetWaitStatus(node, ws, 0);// 将当前结点的等待状态waitStatus设置为初始值0

        /**
         * 检验。
         * 如果，待唤醒结点s为空，或状态为CANCEL（已失效），
         * 则从后尾部往前遍历，循环结束后，将找到最前的一个处于正常阻塞状态的结点。
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }

        /**
         * 若待唤醒结点s不为空，且状态正常，则直接将其唤醒。
         * */
        if (s != null)
            LockSupport.unpark(s.thread);//唤醒线程
    }

    /**
     * 共享模式下唤醒线程：
     * 把当前结点设置为 0 或者 PROPAGATE(-3)
     * 唤醒head.next(B结点)，B结点唤醒后可以竞争锁，成功后head->B，然后又会唤醒B.next，一直重复直到共享结点都唤醒
     *  若 head结点状态为SIGNAL，重置head.waitStatus->0，唤醒head结点线程，唤醒后线程去竞争共享锁
     *  若 head结点状态为0，将head.waitStatus->Node.PROPAGATE传播状态，表示需要将状态向后继结点传播
     */
    private void doReleaseShared() {
        for (;;) {
            Node h = head; // T2记录完旧头结点后，可能T3抢先走完setHead方法，更新头结点指针了，但也可能没更新。
            if (h != null && h != tail) { //队列非空。
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {//只有head是SIGNAL状态,才去唤醒后面的结点
                    /* 唤醒之前，先将head结点waitStatus重置为0，
                       这里不直接设为Node.PROPAGATE,是因为 unparkSuccessor(h)中，
                       如果ws<0;ws会被置回0，所以ws先设置为0，再设置为PROPAGATE
                     *
                       这里需要CAS控制并发，因为入口有setHeadAndPropagate跟release两个，
                       避免两次unpark
                     */
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) //一定要-1状态才去唤醒
                        continue; //如果设置为0失败，重新循环，检查当前情况
                    /* head状态为SIGNAL且成功设置为0之后, 再去唤醒head.next结点线程
                     * 此时head、head.next的线程都唤醒了，head.next会去竞争锁，
                     * head会指向成功获取锁的结点，也就是head发生了变化。
                     * 看最底下一行代码可知，head发生变化后会重新循环，继续唤醒head的下一个结点
                     */
                    unparkSuccessor(h);//唤醒 阻塞在doAcquireShared方法中的线程，被唤醒的线程尝试获取锁，成功后，更新头指针，接着唤醒下下个结点
                }
                /* 当前执行过一次唤醒任务，
                 * 如果旧头结点的waitStatus本身就处于重置状态（waitStatus==0），则将其设置为“传播”状态。
                 * 这意味着，新头结点中‘已唤醒的线程’需要将状态向后一个结点传播
                 */
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            //如果head指针移动了，继续循环。
            if (h == head) //对比旧头指针、新头指针指向的结点是否为同一个结点。
                break; //head指针未更新。(后续没有结点被唤醒)或(被唤醒的结点没有setHead)，跳出循环
            //这里关于头指针，再进一步说明，
            // 释放锁的线程都可以去唤醒头结点下一个结点中的线程，
            // 还没有线程去唤醒时，头部中的state为-1; 有活跃线程执行唤醒后，头部中的state为0;
            // 此时，被唤醒的线程可能执行了setHead，也可能没有，
            // 负责唤醒的线程进入此方法时，记录了旧头结点。
            // h == head 可能成立，可能不成立，这取决于已唤醒线程是否已经执行setHead
            //
            // 负责唤醒的线程T1，
            //   开始记录的一定是旧头结点(T3结点前面的)，如果在 已唤醒线程T3 setHead之前 走完头指针判断
            //     h == head 成立。负责唤醒的线程T1 break;
            //     结束后，可能有 其他负责唤醒的线程T2进来该方法。也可能是 已唤醒的线程T3进来该方法。
            //   开始记录的一定是旧头结点(T3结点前面的)，如果在 已唤醒线程T3 setHead之后,新头结点变为(T3结点/T4结点)，走完头指针判断
            //     h == head 一定不成立。 进入下轮循环，‘尝试修改’旧头(T3结点/T4结点)状态为-3
            // 其他负责唤醒的线程T2
            //   ( 这里，T2一定会走 else if
            //     因为旧头(T3结点前面的)中的state已经被上个负责唤醒的线程T1初始化为0;，
            //     ‘尝试修改’旧头(T3结点前面的)的状态为-3。
            //   )
            //  1.如果T2在 已唤醒线程T3 setHead之前进来，则记录的旧头结点是(T3结点前面的)，
            //    T2于T3 setHead之前走完 头指针判断，T1也先于T3 setHead之前走完 头指针判断。
            //        T1 break;  T2 break;
            //    T2于T3 setHead之前走完 头指针判断，T1于T3 setHead之后走完 头指针判断
            //        T1 一定进入下轮循环
            //          T3 唤醒T4 head可能再次变了
            //          T1 新一轮head记录可能一致，可能不一致。可能改T3结点可能改T4结点
            //               一致 break; 不一致 再次尝试修改状态
            //               ...
            //        T2 break;
            //    T2于T3 setHead之后走完 头指针判断，T1于T3 setHead之前走完 头指针判断
            //        T1 break;
            //        T2 一定进入下轮循环
            //           T3 唤醒T4 head可能再次变了
            //           T2 新一轮记录可能一致，可能不一致。可能改T3结点可能改T4结点
            //               一致 break; 不一致 再次尝试修改状态
            //               ...
            //    T2于T3 setHead之后走完 头指针判断，T1也于T3 setHead之后走完 头指针判断
            //        T1 进入下轮循环
            //           T3 唤醒T4 head可能再次变了
            //           T1 记录可能一致，可能不一致。可能改T3结点可能改T4结点
            //        T2 进入下轮循环
            //           T3 唤醒T4 head可能再次变了
            //           T2 记录可能一致，可能不一致。可能改T3结点可能改T4结点
            //  2.如果T2在 已唤醒线程T3 setHead之后进来，则记录的旧头结点是(T3结点/T4结点)
            //     旧head T3， 新head T3。 一致   break;
            //     旧head T3， 新head T4。 不一致 修改T4(也可能是T5)结点状态;
        }
    }

    /**
     * 把node结点设置成head头结点，且Node.waitStatus->Node.PROPAGATE
     * 注意：该方法执行完成后，旧的头结点并未完全出队。**关键**
     *      进入doReleaseShared方法中的时候，
     *      setHead前，头结点双向指针，
     *      setHead后，头结点单向指针，依然是链表结构！！！
     *      这里与独占锁有很大区别！！！
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; //h用来保存旧的head结点,为了接下来的检查

        //doReleaseShared的if(h == head), 是走在setHead之前还是之后，不确定
        setHead(node);//将head指针指向node结点。更新head头结点指针
        /* ==检查==
          第一项检查：
            在以下情况下尝试向下一个排队的节点发出信号：
         *  1.propagate > 0 && s.isShared()
              propagate > 0;  说明当前线程拿了资源后，还剩余公共资源可以抢
              这意味着，调用方指明了后继结点需要被唤醒。(有人还锁了，你们这些排队等待的可以去抢锁了)
         *  2.waitStatus < 0 && s.isShared()
              通过之前的操作记录了头结点状态(在setHead之前或之后,状态记录在h.waitStatus)
         *    (注意：这里使用了 waitStatus 的符号检查，是因为PROPAGATE状态可能转换为SIGNAL。)
              这里的头结点，无论是 旧的头结点 还是 新的头结点
                旧的头结点(setHead之前的结点)  h == null || h.waitStatus < 0 ;
                         旧头结点waitStatus为-3
                         这里体现了-3的意义。通知头指针所在结点中的线程去唤醒后面的结点。
                新的头结点(setHead之后的结点)  (h = head) == null || h.waitStatus < 0 ;
                         新头结点waitStatus一定为-1
          and 第二项检查：
            (新的头结点的下一个结点在共享模式下等待 or 我们不知道，因为它看起来是空)
          关于以上两项检查的说明：
             这两项检查中的保守性可能会导致不必要的唤醒，
             但这种情况也只有在有多个acquires/releases竞争时才出现。
             因此，无论现在还是不久之后，大多数情况都需要发出唤醒信号。

           h == null 是结点被GC回收的情况？
         */
        if (propagate > 0 || h == null || h.waitStatus < 0 ||
                (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared())//(新头结点node无后继结点) 或 (新头结点node的后继结点是共享结点)
                /* 如果head结点状态为SIGNAL，唤醒head结点的后继结点中的线程，重置head.waitStatus->0
                 * head结点状态为0(第一次添加时是0)，设置head.waitStatus->Node.PROPAGATE表示状态需要向后继结点传播
                 */
                doReleaseShared(); //开始唤醒其他线程之前，head指针一定已经更新完毕。
        }
    }

    // Utilities for various versions of acquire

    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        if (node == null)
            return;

        node.thread = null;

        // Skip cancelled predecessors
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        Node predNext = pred.next;

        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        node.waitStatus = Node.CANCELLED; // **waitStatus置为1**

        // If we are the tail, remove ourselves.
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;
            if (pred != head &&
                    ((ws = pred.waitStatus) == Node.SIGNAL ||
                            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                    pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }

            node.next = node; // help GC  next指向自己
        }
    }

    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * 获取锁资源失败时，进入到这里检查并更新node的waitStatus。
     * 如果线程需要阻塞，返回true。
     * 前驱结点的状态是SIGNAL，当前线程才能放心被阻塞。
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL) //若前驱结点的状态是SIGNAL(信号量为-1)
            /*
               说明前驱结点将在释放锁后，唤醒其后继结点。
               这样，当前线程就可以安心休息了，即，安全的阻塞（前驱节点释放锁时，会唤醒此线程)。
             */
            return true;  //当前结点就可以放心休息(阻塞)了

        if (ws > 0) {//如果前驱结点放弃了(取消状态);信号量>0
            /*
             * 如果前驱结点放弃了(取消状态)，那就一直顺着往前找，
             * 直到找到最近一个正常等待的状态，然后，将当前结点排在它的后边。
             *
             * 注意：那些放弃的结点，它们形成了一个无引用链，稍后就会被垃圾回收器回收了(GC回收)！
             * 图解见文档4.4.1.2（AQS搜索关键字1 ）
             */
            do {
                node.prev = pred = pred.prev; //注意，连续赋值，是从右往左依次进行(更新前驱结点指针，更新当前结点的前驱指针)
            } while (pred.waitStatus > 0); //直至前驱结点状态正常(即 信号量<=0)，才跳出循环。
            pred.next = node;
        } else { //如果前驱结点状态正常 (即 信号量<=0)
            /*
             * 前驱(waitStatus必须 为 0 或 -3 PROPAGATE状态)，
             * 就将 前驱结点(这里的前驱结点也可能是头结点) 设置为SIGNAL状态，相当于告诉前驱结点，我需要信号，即‘执行完任务后’就通知自己一下。
             * 前驱结点信号量标记为-1，
             * 标记完成后，当前结点在本次循环中不阻塞，
             * 在下次循环中若再没获取到锁，则被安全地park(阻塞)。
             * 换句话讲，再次尝试以确保阻塞前，该线程是没有获取到锁的
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL); //***关键步骤*** 将前驱结点信号量置为-1
        }
        return false;  //前驱结点状态更新完毕(无论成功与否，本次都不能阻塞当前结点)
    }

    /**
     * Convenience method to interrupt current thread.
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * 阻塞当前结点；若阻塞后被激活，则返回当前Thread的中断状态
     * LockSupport.park 底层实现逻辑是，调用系统内核功能 pthread_mutex_lock 实现阻塞线程
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);//阻塞当前线程(阻塞状态不会占用CPU资源)。直到被unpark，或者被中断，才会被唤醒。如果是因为中断被唤醒，则下面的interrupted方法返回true
        return Thread.interrupted(); //被唤醒后，返回线程的中断状态。(注意：阻塞了不代表被中断。这一步会清除当前线程的中断标记)
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 已经在等待队列中的Thread结点，准备阻塞等待；若阻塞后被激活，则尝试获取锁
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;  //标记是否成功拿到资源，默认true，失败。
        try {
            boolean interrupted = false; //标记等待过程中是否被中断过，false表示未中断过。
            for (;;) { //又是一个“自旋”！
                final Node p = node.predecessor();//找到‘当前结点’的前驱结点p
                //前驱结点是head，才允许当前结点再次调用tryAcquire 尝试获取锁，
                //其他结点是没有机会调用tryAcquire的。这里体现了公平锁的特性，按先来后到的顺序排队。
                if (p == head && tryAcquire(arg)) {//调用tryAcquire成功,则获取到锁。线程标记为独占。获取到锁的线程不需要在队列中了，因此会出队。
                    setHead(node);//若获取同步状态成功，则将当前结点设置为新的头结点。
                    p.next = null; // setHead中node.prev已置为null，
                          // 此处再将head.next置为null，就是为了方便GC回收 旧的head结点。
                          // 也就意味着之前拿完资源的结点 ‘出队’ 了！
                    failed = false;// 成功获取资源
                    return interrupted; //返回等待过程中是否被中断过
                }
                /**
                 * 如果  ‘前驱结点’不是Head  或 ‘没得到锁’ ，则进行阻塞，进入waiting状态，
                 * 具体是通过以下两个方法判断实现：
                 * 1）shouldParkAfterFailedAcquire方法：返回 是否需要阻塞当前线程
                 * 2）parkAndCheckInterrupt方法：阻塞当前线程。线程暂停在该方法中
                 *            当前线程再次唤醒时，方法继续执行完。
                 *            执行完方法后，返回 当前线程是否成功中断。
                 *
                 * 通过shouldParkAfterFailedAcquire判断是否应该阻塞当前线程
                 * 判断方式：‘前驱结点’的‘信号量’(waitStatus) 为-1时，则判断当前结点可以阻塞。
                 *
                 * 此时，当前线程可以被parkAndCheckInterrupt函数安全地阻塞
                 */
                if (shouldParkAfterFailedAcquire(p, node) &&
                                  // 如果这里不去阻塞，那么，一个线程进来后会一直死循环，直至拿到锁。这样就非常浪费cpu资源。
                        parkAndCheckInterrupt())
                                  // 如果阻塞，则当前线程会暂停在这里。不再往下执行。
                    interrupted = true;//如果等待过程中被要求中断，哪怕只有那么一次，
                                       // 就将interrupted 赋值 true。
                                       // 将当前结点，标记为被中断过，以便后续补上中断。
            }
        } finally {
            if (failed)
                //如果等待过程中(跳出自旋后)依然没有成功获取资源（如：timeout，或者 ‘在可中断的情况下被中断了’），
                //那么,取消结点在队列中的等待(出队)。将此线程对应的node的waitStatus改为CANCELLED
                cancelAcquire(node);
        }
    }

    /**
     * 与acquireQueued的逻辑 大同小异。
     * 区别：
     *     1.这里的结点还不在同步队列当中，需要先进行入队操作。
     *     2.若检测到线程被中断，则直接抛出异常。而不是继续循环。
     */
    private void doAcquireInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);//区别：以独占模式放入队列尾部。condition只能是独占模式
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();//区别：中断直接抛异常
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 独占模式定时获取
     */
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);//加入队列
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                // 时间判断
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;//超时直接返回获取失败
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    //阻塞指定时长，超时则线程自动被唤醒
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())//当前线程中断状态
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * 执行获取共享锁操作
     */
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);//入队CLH
        boolean failed = true; //是否获取成功。默认为失败
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();//前驱结点
                if (p == head) {
                    int r = tryAcquireShared(arg); //开始尝试获取锁
                    // state==0时tryAcquireShared会返回>=0(CountDownLatch中返回的是1)。
                    // state为0说明共享次数已经到了，可以获取锁了
                    if (r >= 0) {//r>0表示state==0,前继结点已经释放锁，锁的状态为可被获取
                        //这一步设置node为head结点设置node.waitStatus->Node.PROPAGATE，然后唤醒node.thread
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                //前继结点非head结点，将前继结点状态设置为SIGNAL，通过park挂起node结点的线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);//取消尝试获取锁的操作。
        }
    }

    /**
     * Acquires in shared interruptible mode.
     * @param arg the acquire argument
     */
    private void doAcquireSharedInterruptibly(int arg)
            throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);//以SHARED共享模式，入队CLH。新加入的结点默认信号量是0
        boolean failed = true; //默认获取锁失败。
        try {
            for (;;) {
                final Node p = node.predecessor(); //获取前驱结点
                if (p == head) {  // 前驱结点为头结点的结点才有机会获取锁。否则，会在下个if中被park。
                    int r = tryAcquireShared(arg);//入队完成后，先尝试获取共享锁资源。
                    //Semaphore中，到这一刻的时候，可能其他线程抢到了锁、也可能释放了锁。此时r值并未更新。
                    //所以，这里的r值，只能作为判断是否接着唤醒下个结点的依据，并不能代表state的真实值。
                    if (r >= 0) { // r: remaining 余额
                        setHeadAndPropagate(node, r);//获取到锁资源后，并没有急着去执行任务，而是在重置头结点指针后，接着唤醒下一个结点中的线程
                        p.next = null; // help GC 线程执行唤醒任务后，才完全剔除头结点
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException(); // 这里有别于 doAcquireShared方法。会抛异常
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // Main exported methods

    /**
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in exclusive
     * mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 共享式：共享式地获取同步状态。对于独占式同步组件来讲，同一时刻只有一个线程能获取到同步状态，
     * 其他线程都得去排队等待，其待重写的尝试获取同步状态的方法tryAcquire返回值为boolean，这很容易理解；
     * 对于共享式同步组件来讲，同一时刻可以有多个线程同时获取到同步状态，这也是“共享”的意义所在。
     * 本方法待被之类覆盖实现具体逻辑
     *  1.当返回值大于0时，表示获取同步状态成功，同时还有剩余同步状态可供其他线程获取；
     *
     *　2.当返回值等于0时，表示获取同步状态成功，但没有可用同步状态了；
     *
     *　3.当返回值小于0时，表示获取同步状态失败。
     */
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to set the state to reflect a release in shared mode.
     *
     * <p>This method is always invoked by the thread performing release.
     *
     * <p>The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     *
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     *
     * 此方法是 “独占模式” 下，线程‘获取共享资源’的顶层入口。
     * 如果获取到资源，线程直接返回，否则进入同步等待队列，直到获取到资源为止，
     *    整个过程忽略中断的影响。
     * 这也正是lock()的语义，当然不仅仅只限于lock()。
     *    获取到资源后，线程就可以去执行其‘临界区’代码了。
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */ // acquire vt. 获得；取得；
    public final void acquire(int arg) {
        //tryAcquire()尝试获取锁，如果成功则直接返回true。该方法在AQS中只是个接口，具体实现交给自定义同步器
        if (!tryAcquire(arg) && // tryAcquire有FairSync/NonfairSync两种实现。
                //获取不到锁的线程,则进入等待队列，并返回是否被中断，
                //具体步骤如下：
                // 1. addWaiter()
                //    将该线程加入等待队列的尾部，并标记为独占模式；将线程包装成node后，返回。
                // 2. acquireQueued()
                //    使线程阻塞在等待队列中一直获取资源，直到获取到资源后才返回。
                //    如果在整个等待过程中被标记中断，则返回true，否则返回false。
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            //线程在整个等待过程中被标记中断，则进入这里
            //即使线程在等待过程中被标记中断，实际也并没有真正中断线程。所以，这里它是不响应的。
            //原因是Thread.interrupted()会清除当前线程的中断标记位；
            //所以，需要在这里(获取到资源后)，通过自我中断selfInterrupt()的方式将中断标记补上。
            selfInterrupt();
    }

    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())  //如果线程获取锁之前被中断，则抛出异常
            throw new InterruptedException();
        if (!tryAcquire(arg))  // 获取锁失败
            doAcquireInterruptibly(arg); //进入同步队列。循环获取锁
    }

    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
                doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 释放独占模式持有的锁
     */
    public final boolean release(int arg) {
        //修改锁计数器，如果锁计数器为0,说明锁被释放
        //tryRelease在com.it.edu.aqs.ReentrantLock.Sync#tryRelease中实现
        if (tryRelease(arg)) {//每进来一次，释放一次锁。释放成功，进入方法
            Node h = head;
            //head节点的waitStatus不等于0，说明 head节点的后继节点 对应的线程 待唤醒
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);//唤醒等待队列中的第一个结点
            return true;
        }
        return false;
    }

    /**
     * 请求获取共享锁
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)//返回值小于0，获取同步状态失败，排队去；获取同步状态成功，直接返回去干自己的事儿。
            doAcquireShared(arg);
    }

    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0) // com.it.edu.aqs.Semaphore.NonfairSync#tryAcquireShared
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 ||
                doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) { //释放锁资源
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire
     */
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention
     */
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails
     */
    private Thread fullGetFirstQueuedThread() {
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;
        if (((h = head) != null && (s = h.next) != null &&
                s.prev == head && (st = s.thread) != null) ||
                ((h = head) != null && (s = h.next) != null &&
                        s.prev == head && (st = s.thread) != null))
            return st;

        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */

        Node t = tail;
        Thread firstThread = null;
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }
        return firstThread;
    }

    /**
     * 判断当前线程是否在队列当中
     */
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null &&
                (s = h.next)  != null &&
                !s.isShared()         &&
                s.thread != null;
    }

    /**
     * 查询是否已经有比 当前线程 等待时间更长的线程 在排队等待获取锁。
     *
     * 结合enq()方法来分析。
     * com.it.edu.aqs.AbstractQueuedSynchronizer#enq(com.it.edu.aqs.AbstractQueuedSynchronizer.Node)
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s; //准备入队的结点
        return h != t &&  //1. 头结点 不等于 尾结点(非空队列)。若h == t，则说明当下队列为空。
                ((s = h.next) == null // 2. true  说明当前等待队列正在被初始化。(有一个线程正在进行入队操作才会触发队列的初始化操作)
                                      //    false 说明等待队列已经初始化完成,且有线程已经入队。
                        || s.thread != Thread.currentThread());
                        // 3.第2步为false时，走到这一步。
        // 接着我们就判断，这个在等待队列中，第一个等待获取锁的线程，是不是当前线程。
        // 『s.thread != Thread.currentThread()』，
        // 如果是当前线程的话，方法结束并返回false，表示当前线程前面没有比它等待更久获取这个锁的线程了；
        // 不是当前线程，方法结束返回true，表示当前线程前面有比它等待更久希望获取这个锁的线程。
    }


    // Instrumentation and monitoring methods

    /**
     * 同步队列长度
     */
    public final int getQueueLength() {
        int n = 0;
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * 获取队列等待thread集合
     */
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * 获取独占模式等待thread线程集合
     */
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 获取共享模式等待thread集合
     */
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
                "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions

    /**
     * 判断结点是否在同步队列中
     *  false 表示不在同步队列中，而在条件队列中
     *  true  表示在同步队列中，不在条件队列中。
     */
    final boolean isOnSyncQueue(Node node) {
        //快速判断1：结点状态为CONDITION 或 结点没有前置结点
          //注：同步队列是有头结点的，而条件队列没有
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;
        //快速判断2：next字段只有同步队列才会使用，条件队列中使用的是nextWaiter字段
        if (node.next != null) // If has successor, it must be on queue
            return true;
        //上面如果无法判断则进入复杂判断
        return findNodeFromTail(node);
    }

    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * 从尾结点开始逆向搜索，如果 “当前结点” 在 ‘同步队列’ 上，则返回true。
     * Called only when needed by isOnSyncQueue.
     * 仅在isOnSyncQueue方法需要时调用。
     * @return true if present
     *         如果存在，返回true
     */
    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (;;) {
            if (t == node)     //同步队列中，从尾结点开始搜，搜索到了当前结点
                return true;
            if (t == null)     //同步队列中没有搜索到当前结点。
                return false;
            t = t.prev;  //搜索指针 前移
        }
    }

    /**
     * 将结点 从‘条件队列’当中移动到‘同步队列’当中，等待获取锁
     * 本方法在临界区当中
     */
    final boolean transferForSignal(Node node) {
        /*
         * 1.CAS操作，将当前结点信号量状态,从条件等待状态修改为0，操作失败直接返回false
         */
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;
        /*
         * 2.将当前结点加入到同步队列尾部当中，返回CHL中，当前结点的前驱结点
         */
        Node p = enq(node);
        int ws = p.waitStatus;
        //当前结点node的前驱结点p 无效状态 或者 CAS修改'前驱结点'为通知状态失败
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);  //则 唤醒当前结点中的线程(不能安心休眠)。
        return true;
    }

    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     * 在取消等待后，转移结点(如果需要)到同步队列。
     * 如果线程在其他线程发出信号之前被取消，则返回true。
     * @param node the node
     * @return true if cancelled before the node was signalled
     */
    final boolean transferAfterCancelledWait(Node node) {
        //正常来讲，当前node被转移到CHL队列，才会跳出循环。并且结点状态为0;
        // 在这里CAS修改结点状态成功。说明结点被中断的时候，信号量是CONDITION条件等待状态
        // (线程在发出信号前被中断。这种情况是异常的。)
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);    //则将结点加入同步队列。
            return true;  //异常中断
        }
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         * 结点在  未完成转义 的过程当中 被取消 ，是罕见的 也是短暂的，所以只需自旋。
         * 在它完成同步队列的入队操作之前，我们都不能继续执行
         */
        //CAS修改结点状态失败，说明结点被中断的时候，信号量已经不是CONDITION状态。(线程在发出信号过程被中断)
        while (!isOnSyncQueue(node))//如果结点不在‘同步队列’中。
                                    //说明在条件队列中 (此时，不具有被唤醒的权利)
            Thread.yield(); //让当前线程自旋，一直让步资源。
                           // 直到signal方法执行完成，使之成功入队同步队列，才跳出循环
        return false; //CAS修改结点状态失败。说明结点被中断的时候信号量不是CONDITION状态
    }

    /**
     * 方法参数是新创建的结点，即当前结点
     */
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            //这里这个取值要注意，获取当前的state并释放，这从另一个角度说明必须是独占锁
            //可以考虑下这个逻辑放在共享锁下面会发生什么？
            int savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                //如果这里释放失败，则抛出异常
                throw new IllegalMonitorStateException();
            }
        } finally {
            /**
             * 如果释放锁失败，则把结点取消，由这里就能看出来，上面添加结点的逻辑中
             * 只需要判断最后一个结点是否被取消就可以了
             */
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions

    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    /**
     * 获取条件队列长度
     */
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * 获取条件队列当中所有等待的thread集合
     */
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitingThreads();
    }

    /**
     * ConditionObject 条件对象
     * ，实现基于条件的具体行为
     */
    public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        /** First node of condition queue. */
        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        public ConditionObject() { }

        // Internal methods

        /**
         * 1.与同步队列不同，条件队列的头尾指针分别是firstWaiter跟lastWaiter
         * 2.入队条件队列的操作是在 获取锁之后（也就是临界区）进行的，
         *   因此，本方法中很多地方都不用考虑并发问题
         */
        private Node addConditionWaiter() {
            Node t = lastWaiter;  //获取条件队列中的尾结点。
            //如果条件队列中的 尾结点不为空 并且 该结点信号量不为CONDITION(-2)。则判断为该尾结点被取消。
            //如果尾结点被取消，则删除队列中这个被取消的结点
            //至于为啥是最后一个结点，后面会分析。图解AQS 4.2.2 小节。 图2
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();//删除所有被取消的结点
                t = lastWaiter; //再次获取尾结点。
            }
            //创建一个状态为CONDITION的结点并加入条件队列，由于在临界区，所以这里不用并发控制
            Node node = new Node(Thread.currentThread(), Node.CONDITION);//**关键**
            if (t == null)  //尾结点t为null，说明当前条件队列为空。
                firstWaiter = node;  //则需要更新头指针。
            else //当前条件队列不为空
                t.nextWaiter = node;//则直接将新的结点加入条件队列的队尾。完成入队。
                                    // 一个指针操作就完成了入队。可看出是个从头指到尾的单向链表。
            lastWaiter = node; //将尾指针指向新的结点。(更新尾指针)
            return node;  //返回新创建的结点
        }

        /**
         * 发信号，遍历条件队列，通知条件队列当中的头结点转移到同步队列当中，准备排队获取锁
         * 本方法在临界区当中
         */
        private void doSignal(Node first) {
            do {
                if ( (firstWaiter = first.nextWaiter) == null) //更新条件队列头指针，指向头结点的后继结点。
                    lastWaiter = null;  //如果头结点的后继结点为空(说明队列为空)，则更新条件队列尾指针为空
                first.nextWaiter = null; //头结点出队条件队列
            } while (!transferForSignal(first) && //**关键** 将出队后的结点(游离状态)，入队同步队列。入队成功，则停止循环
                    (first = firstWaiter) != null);//入队失败，则获取新的头结点，新的头结点有效(不为空),即进入下轮循环。
                                                    //上一步出队的结点，处于游离状态。等待GC回收。
        }

        /**
         * 通知所有结点移动到同步队列当中，并将结点从条件队列删除
         */
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null; //头、尾指针均置空
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;  //头结点出队条件队列
                transferForSignal(first); //游离结点入队同步队列，待被通知唤醒
                first = next;
            } while (first != null);
        }

        /**
         * 删除条件队列当中被取消的结点（t != null && t.waitStatus != Node.CONDITION）
         * 将条件队列中所有waitStatus不等于 -2( CONDITION ) 的结点 移出队列
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter; //t代表当前正在检查的结点(检查指针)。初始值为头结点(从头结点开始检查)
            Node trail = null;//trail代表‘nextWaiter字段等于t节点’的结点(暂理解为前驱结点)
            while (t != null) {
                Node next = t.nextWaiter; //结点next 暂理解为后继结点

                //t结点的信号量不为 CONDITION 条件等待状态(出队条件)
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;  //将t结点的后继结点指针置为空(即将出队)
                    if (trail == null) //前驱结点为空，说明t是头结点
                        firstWaiter = next;//则将头结点指针指向后继结点。(更新头指针，完成出队)
                    else               //前驱结点不为空，说明t不是头结点
                        trail.nextWaiter = next; //则将前驱结点的后继结点指针指向t结点的后继结点。(完成出队)
                    if (next == null)  //后继结点next为空，说明当前结点t是尾结点。
                        lastWaiter = trail; //然而此时，当前结点t已完成出队，所以将尾结点指针指向t的前驱结点trail。(更新尾指针)
                }
                //如果当前结点是 CONDITION 条件等待状态
                else
                    trail = t;//则不需要作处理，直接将当前t结点更新为前驱结点。准备开始下轮
                t = next; //开始下一轮之前，先将原后继结点，置为新的当前结点t。(更新检查指针)
            }
        }

        // public methods

        /**
         * 发信号，通知条件队列中的结点到同步队列中排队
         * 本方法在临界区当中。
         */
        public final void signal() {
            if (!isHeldExclusively()) //当前线程需持有独占锁，否则抛异常（以保证本方法中的后续操作在临界区）
                throw new IllegalMonitorStateException();
            Node first = firstWaiter; //条件队列中的头结点
            if (first != null)
                /**
                 * 发信号通知条件队列的结点，准备到同步队列当中去排队
                 */
                doSignal(first);
        }

        /**
         * 唤醒所有条件队列的结点转移到同步队列当中
         */
        public final void signalAll() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null)
                doSignalAll(first);
        }

        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        /** 该模式表示，线程对象在退出条件等待时，重新标记中断(恢复中断状态) */
        private static final int REINTERRUPT =  1; //reintroduce 再引入
        /** 该模式表示，线程对象在退出条件等待时，异常中断(抛出异常) */
        private static final int THROW_IE    = -1;

        /**
         * 等待时检查中断。
         * 判断逻辑是：
         * 1.如果结点没有被中断，则说明结点正常，处于等待被signal唤醒状态，返回0
         * 2.如果结点被中断，
         *    a.Condition状态，则确认线程未被signal，而是直接被中断interrupt()唤醒。返回 THROW_IE (后续处理 会抛中断异常)
         *    b.不是Condition状态，则说明线程是signal过程中被中断interrupt()唤醒。返回 REINTERRUPT(完成signal后，后续处理 会补上中断标识)
         */
        private int checkInterruptWhileWaiting(Node node) {
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }

        /**
         * 根据中断时机（signal之前、signal过程中） 选择 是抛出异常 还是 重新设置线程中断状态
         */
        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }

        /**
         * 将 当前线程 入队条件队列的操作，所调用的顶层入口
         * 将当前线程加入条件队列，阻塞等待。
         */
        public final void await() throws InterruptedException {
            //如果当前线程入条件队列前被中断则直接抛出异常
            if (Thread.interrupted())
                throw new InterruptedException();

            //**关键步骤** 把当前线程封装到新的结点中，然后‘加入条件队列’。并返回当前结点
            Node node = addConditionWaiter();

            //**关键步骤** 入队条件队列完成后 解锁。
                //释放当前结点已获取的全部独占锁资源(当前线程是持有独占锁的);
                //后续其他线程进入条件队列等待之前。肯定是要当前线程释放独占锁的。
                //否则会造成死锁。
                //具体为，当前线程拿着锁在条件队列中等待。后续其他线程无法获取锁，进而无法进入临界区。
            int savedState = fullyRelease(node);
            int interruptMode = 0; //中断模式: 抛异常 或 重新设置中断标记
            //如果 当前结点 不在‘同步队列’中，则不断被挂起park(阻塞)
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this); //**关键步骤** 如果当前线程不在同步队列中，则被阻塞

                // 运行到这里，说明线程被唤醒。 被唤醒后，接下来还需要确定中断模式(未中断，或其他)
                //   如果再次被唤醒，则可能是因为‘正常的signal操作’也可能是因为‘被中断’，
                //   总结下来，有下三种情况导致被唤醒： '-->' 表示 '调用'
                // （1）线程在同步队列中（正常流程）。   满足退出循环条件
                //     满足条件后‘其它线程’-->signal/signalAll方法，当前线程加入同步队列。然后被正常唤醒。
                // （2）线程不在同步队列中，但是被中断。 直接跳出循环
                //      a)Condition状态。    在signal前被中断
                //      b)不是Condition状态。 在signal过程中被中断
                //     说明：‘其它线程’-->‘interrupt当前线程’，使当前线程会被唤醒。

                //检查中断。0、1、-1
                // interruptMode=0，则继续走while的判断条件。
                // interruptMode!=0 暴力跳出循环
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            /**
             * 走到这里，说明结点 ‘因满足条件而移出了条件队列，并且 成功加入到了‘同步队列’ 或 ‘被中断’
             *
             * 在处理中断之前，首先要做的是，从‘同步队列’中成功‘获取锁’(acquireQueued)资源
             *   acquireQueued方法很熟悉吧？获取锁的方法就跟独占锁调用一样
             *   从这里可以看出 ‘条件队列’ 只能用于 ‘独占锁’
             */
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
                    //走到这里说明当前结点是CLH等待队列中的头结点
                    //且已经成功获取到了独占锁，并已确定好中断模式，
                    // 中断状态无论是0,1这里都要改为1。因为过程中已经被park中断过一次。
                    // 接下来就做些收尾工作
            if (node.nextWaiter != null)  // clean up if cancelled
                unlinkCancelledWaiters(); //收尾工作1：删除条件队列中被取消的结点
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
                                         //收尾工作2：根据不同模式处理中断。
                                         //  抛异常 或 中断标记
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();    //当前线程加入条件队列
            int savedState = fullyRelease(node); //释放独占锁
            final long deadline = System.nanoTime() + nanosTimeout; //等待结束的时间戳
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) { //如果当前线程不在同步队列(说明在条件队列中)
                // 如果，满足 结束等待的条件，
                if (nanosTimeout <= 0L) {
                    // 则移出条件队列，加入到同步队列
                    transferAfterCancelledWait(node);
                    break;
                }

                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);

                // 检查中断。
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;

                // 如果 0 < nanosTimeout < spinForTimeoutThreshold。
                // 则，通过自旋实现等待，直到 nanosTimeout <= 0
                nanosTimeout = deadline - System.nanoTime();
            }
            //下面三个判断的详细注释同  await()；
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }

        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean awaitUntil(Date deadline)
                throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        public final boolean await(long time, TimeUnit unit)
                throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted())
                throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return !timedout;
        }

        //  support for instrumentation

        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final boolean hasWaiters() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }
            return false;
        }

        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        protected final int getWaitQueueLength() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        /**
         * 得到同步队列当中所有在等待的Thread集合
         */
        protected final Collection<Thread> getWaitingThreads() {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     * unsafe魔法类，直接绕过虚拟机内存管理机制，修改内存
     */
    //private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final Unsafe unsafe = UnsafeInstance.reflectGetUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS 修改头部结点指向. 并发入队时使用.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS 修改尾部结点指向. 并发入队时使用.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
//        System.out.println("compareAndSetTail this --------"+this);
// --------com.it.edu.aqs.ReentrantLock$FairSync@46ebab1a[State = 1, empty queue]
// 可以发现，这里的 this，是公平锁的实现，即FairSync。

        //arg1:待操作对象，arg2:对象的属性 指针位置，arg3:当前结点，arg4:尾结点。

        //arg1 与arg2 。可以是 FairSync对象 和 ‘尾指针’的内存地址。
        //arg1 与arg2 就可以确定当前对象指定属性对应的值，即tail属性对应的值。
        //然后将 尾指针指向的对象 与 agr3(期望) 进行比较，
        // 相等，  则通过CAS操作 将‘尾指针’指向arg4(待更新)。| 相等，说明‘尾指针’是指向传入的结点
        // 不相等，则cas操作失败，返回false       | 不相等，说明‘尾指针’不是指向传入的结点
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS 修改信号量状态.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        //Unsafe类中的compareAndSwapInt，是一个本地方法，该方法的实现位于unsafe.cpp中。
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }

    /**
     * 修改结点的后继指针.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
