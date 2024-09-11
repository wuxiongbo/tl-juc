package com.it.edu.aqs;
/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 *
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 *
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /**
     * 内部调用AQS的动作，都基于该成员属性实现
     */
    private final Sync sync;

    /**
     * 封装了 ReentrantLock锁 同步操作的 基础抽象类，继承自AQS框架.
     * 该类有两个继承子类，1、NonfairSync 非公平锁，2、FairSync公平锁
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * 加锁的具体行为由子类实现(FairSync、NonfairSync 公平、非公平)
         */
        abstract void lock();

        /**
         * 尝试获取非公平锁
         */
        final boolean nonfairTryAcquire(int acquires) {
            //acquires = 1
            final Thread current = Thread.currentThread();
            //volatile读，确保了 锁状态位 的内存可见性
            int c = getState();
            /**
             * 不需要判断同步队列（CLH）中，是否有正在排队的等待线程
             *
             * 判断state状态是否为0，
             * 为0 表示锁还没有被其他线程占用
             * 此时，可以加锁
             */
            if (c == 0) {
                //unsafe操作，CAS操作修改state状态
                //此时，如果多个线程同时进入，CAS操作会确保只有一个线程修改成功
                if (compareAndSetState(0, acquires)) {
                    //独占状态锁标记指针 指向当前线程，即设置当前线程拥有独占访问权
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            /**
             * state状态不为0，
             * 则进一步判断锁的持有者是否为当前线程，
             * 如果是当前线程持有，当前线程就是拥有独占访问权的线程，即锁重入，
             * 则state+1
             */
            else if (current == getExclusiveOwnerThread()) {
                //重入锁计数+1
                int nextc = c + acquires;
                if (nextc < 0) // overflow 溢出
                    throw new Error("Maximum lock count exceeded");
                //只有获取锁的线程，才能进入此段代码，因此只需要一个volatile写操作，确保其内存可见性即可
                setState(nextc);
                return true;
            }
            //加锁失败
            return false;
        }

        /**
         * 释放锁
         */
        //只有获取锁的线程才会执行此方法，因此只需要volatile读/写，确保内存可见性即可
        @Override
        protected final boolean tryRelease(int releases) {
            //锁计数器-1
            int c = getState() - releases; //volatile读
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false; //表示锁是否被释放
            //锁计数器为0，说明锁被释放
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);//volatile写
            return free;
        }
        //线程安全的关键：volatile变量和CAS原语(Atomic Hardware Primitives 硬件同步原语)的配合使用

        /**
         * 判断持有独占锁的线程是否是当前线程
         */
        @Override
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        //返回条件对象
        final ConditionObject newCondition() {
            return new ConditionObject();
        }


        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * 非公平锁
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;
        /**
         * 加锁行为
         */
        @Override
        final void lock() {
            //这里体现了非公平锁特性，上来就直接尝试获取锁。
            // 不管CLH队列中，是否有别的线程在等待，直接进行一次插队；
            /**
             * 这个if分支，多了一次直接尝试加锁（cas操作）的机会
             * 与公平锁实现的加锁行为一个最大的区别在于:
             *    此处不会去判断同步队列(CLH队列)中是否有排队等待加锁的节点，
             *    上来直接加锁（判断state是否为 期望值0，是，则CAS修改state为1），
             *    {@link com.it.edu.aqs.AbstractQueuedSynchronizer#state}
             *    并将独占锁持有者 exclusiveOwnerThread 属性指向当前线程.
             *
             * 只有一次插队机会，如果本次抢锁失败，则老老实实去排队(而非继续插队)。
             */
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread()); //与共享锁的区别，独占锁会独占线程进行标识。
            else
                //AQS定义的方法,加锁
                acquire(1);
        }

        /**
         * 父类AbstractQueuedSynchronizer.acquire()中调用本方法
         */
        @Override
        protected final boolean tryAcquire(int acquires) {

            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        @Override
        final void lock() {
            acquire(1);
                //此方法是独占模式下，线程获取共享资源的顶层入口。
                //如果获取到资源，线程直接返回。否则进入等待队列，直到获取到资源为止。**且整个过程忽略中断的影响**。
        }

        /**
         * 重写AQS中的方法逻辑(AQS中是空方法)
         * 尝试加锁，被AQS的acquire()方法调用
         */
        @Override
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {  //锁状态为零，表示目前没有任何线程加锁。
                /**
                 * 这里就是与非公平锁中的区别！！
                 * 在公平锁这里，需要先通过hasQueuedPredecessors()方法判断队列当中是否有等待的结点
                 * 队列中没有等待结点，当前线程才可以尝试CAS获取锁。
                 * 否则，说明有人排队在先，当前线程老老实实到后面排队去。
                 */
                if (!hasQueuedPredecessors() && //判断队列中是否已经有在等待的结点。
                        //没有等待结点，则通过cas原子操作获取锁。state为0，则获取成功。
                        compareAndSetState(0, acquires)) {
                    //成功获取锁后，将独占线程的指针 指向当前线程，进行标识。
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) { //如果当前线程为独占线程
                int nextc = c + acquires;  //允许独占线程再一次加锁，这里体现了 ‘可重入’ 的特性。（这里的可重入性是针对独占线程的）
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
            //锁状态不为零(锁已经被拿) 且 当前线程不是独占线程。(说明已经有线程抢先拿到锁了)
            //锁状态为零，但是 队列中有等待的结点 或 CAS失败(与其他线程竞争锁失败)。
        }
    }

    /**
     * 默认构造函数，创建非公平锁对象
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 根据要求创建公平锁或非公平锁
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 尝试获取锁。若获取失败，则被阻塞;
     */
    @Override
    public void lock() {
        sync.lock();// lock有FairSync/NonfairSync两种实现。
    }

    /**
     * 尝试获取锁。若获取失败，则被阻塞; 若线程被中断，则直接抛出异常
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 尝试加锁
     */
    @Override
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 指定等待时间内尝试加锁
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 尝试去释放锁
     */
    @Override
    public void unlock() {
        sync.release(1);
    }

    /**
     * 返回条件对象
     */
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 返回当前线程持有的state状态数量
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 查询当前线程是否持有锁
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * 状态表示是否被Thread加锁持有
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * 是否公平锁？是返回true 否则返回 false
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns this lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * 判断队列当中是否有在等待获取锁的Thread节点
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * 当前线程是否在同步队列中等待
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire this lock.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring of the system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * 返回Thread集合，排队中的所有节点Thread会被返回
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * 条件队列当中是否有正在等待的节点
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    @Override
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
