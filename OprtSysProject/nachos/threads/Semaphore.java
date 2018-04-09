package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Semaphore</tt> is a synchronization primitive with an unsigned value. A
 * semaphore has only two operations:
 *
 * <ul>
 * <li><tt>P()</tt>: waits until the semaphore's value is greater than zero,
 * then decrements it.
 * <li><tt>V()</tt>: increments the semaphore's value, and wakes up one thread
 * waiting in <tt>P()</tt> if possible.
 * </ul>
 *
 * <p>
 * Note that this API does not allow a thread to read the value of the semaphore
 * directly. Even if you did read the value, the only thing you would know is
 * what the value used to be. You don't know what the value is now, because by
 * the time you get the value, a context switch might have occurred, and some
 * other thread might have called <tt>P()</tt> or <tt>V()</tt>, so the true
 * value might now be different.
 */
public class Semaphore {
	/**
	 * Allocate a new semaphore.
	 *
	 * @param initialValue
	 *            the initial value of this semaphore.
	 */
	public Semaphore(int initialValue) {
		value = initialValue;
	}

	/**
	 * Atomically wait for this semaphore to become non-zero and decrement it.
	 */
	public void P() {
		boolean intStatus = Machine.interrupt().disable();

		if (value == 0) {
			waitQueue.waitForAccess(KThread.currentThread());
			KThread.sleep();
		} else {
			value--;
		}

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Atomically increment this semaphore and wake up at most one other thread
	 * sleeping on this semaphore.
	 */
	public void V() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = waitQueue.nextThread();// the WaitQueue defined in
												// Nachos.semaphore
		if (thread != null) {
			thread.ready();
		} else {
			value++;
		}

		Machine.interrupt().restore(intStatus);
	}

	private static class PingTest implements Runnable {
		PingTest(Semaphore ping, Semaphore pong) {
			this.ping = ping;
			this.pong = pong;
		}

		public void run() {
			for (int i = 0; i < 10; i++) {
				ping.P();
				System.out.println("*** thread 1 run "+i+" times");
				pong.V();
			}
		}

		private Semaphore ping;
		private Semaphore pong;
	}
	
	private static class ConditionTest implements Runnable {
		ConditionTest(Lock l_mutex,Condition2 cond) {
			this.l_mutex=l_mutex;
			this.cond=cond;
		}

		public void run() {
			for (int i = 0; i < 10; i++) {
				System.out.println("*** Thread "+KThread.currentThread().getName()+" run "+i+" times");
			}
			l_mutex.acquire();
			conditionStatus=true;
			cond.wake();
			l_mutex.release();
			
			
		}

		private Lock l_mutex;
		private Condition2 cond;
	}

	/**
	 * Test if this module is working.
	 */
	public static void selfTest() {
//		Semaphore ping = new Semaphore(0);
//		Semaphore pong = new Semaphore(0);
//
//		new KThread(new PingTest(ping, pong)).setName("ping").fork();
//
//		for (int i = 0; i < 10; i++) {
//			ping.V();
//			System.out.println("*** Thread 0 run "+i+" times");
//			pong.P();
//		}
		/***Phase 1.Task2.test*/
		Lock mutex=new Lock();
		Condition2 cond=new Condition2(mutex);
		KThread newThread = new KThread(new ConditionTest(mutex,cond)).setName("Condition");
		newThread.fork();
		System.out.println("condition status : "+conditionStatus);
		mutex.acquire();
		while(!conditionStatus){
			cond.sleep();
		}
		for(int i=0;i<10&&conditionStatus;++i)
				System.out.println("*** Thread "+KThread.currentThread().getName()+" run "+i+" times");
		mutex.release();
		/***Phase 1.Task3.test*/
		System.out.println("*** Thread : "+KThread.currentThread().getName()
				+" fall in sleep in : "+Machine.timer().getTime());
		ThreadedKernel.alarm.waitUntil(1020);//current Thread sleep 1020 ticks 
		System.out.println("*** Thread : "+KThread.currentThread().getName()
				+" wake up in : "+Machine.timer().getTime());
	}

	private int value;
	private static boolean conditionStatus=false;
	private ThreadQueue waitQueue = ThreadedKernel.scheduler
			.newThreadQueue(false);
}
