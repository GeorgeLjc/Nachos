package nachos.threads;

import nachos.machine.*;
import nachos.security.Privilege;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		boolean initstatus = Machine.interrupt().disable();
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
		Machine.interrupt().restore(initstatus);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}
	
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	
	public static void  dynamic_selftest(){
		PriorityScheduler scheduler =new PriorityScheduler();
		Lock recource =new Lock();
		KThread Pritest1 =new KThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				recource.acquire();
				System.out.println("KThread Pritest 1 yeild()");
				KThread.yield();//let Pritest1 back to ReadyQue holding the lock
				for(int i=0;i<5;++i){
					System.out.println("**** "+"Pritest 1 run "+i+" times ");
				}
				System.out.println("KThread "+ KThread.currentThread().getName()+" finish ");
				recource.release();
			}

		}).setName("Pritest 1");
		scheduler.setPriority(Pritest1, 1);//deflaut_priority
		Pritest1.fork();
		System.out.println("KThread "+Pritest1.getName()+" fork()");
		System.out.println("Kthread main yeild 1st time");
		KThread.yield();//let Pritest1 run
		System.out.println("KThread "+KThread.currentThread().getName()+" back ");
		KThread Pritest2 =new KThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				recource.acquire();
				for(int i=0;i<5;++i){
					System.out.println("**** "+"Pritest 2 run "+i+" times ");
				}
				System.out.println("KThread "+ KThread.currentThread().getName()+" finish ");
				recource.release();
			}

		}).setName("Pritest 2");
		scheduler.setPriority(Pritest2,5);//midel_priority
		Pritest2.fork();
		System.out.println("KThread Pritest 2 fork()");
		
		KThread Pritest3 =new KThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				recource.acquire();
				for(int i=0;i<5;++i){
					System.out.println("**** "+"Pritest 3 run "+i+" times ");
				}
				System.out.println("KThread "+ KThread.currentThread().getName()+" finish ");
				recource.release();
			}

		}).setName("Pritest 3");
		scheduler.setPriority(Pritest3,7);//high_priority
		Pritest3.fork();
		System.out.println("KThread Pritest 3 fork()");
		System.out.println("KThread main yield() 2nd time");
		KThread.yield();//let thread Pritest1 Pritest2 Pritest3 run
		System.out.println("KThread main back");
	}
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
			ResourceHolder=thread;
			
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if(WaitQueue.isEmpty())
				return null;
			ThreadState NextThreadStates = pickNextThread();
			NextThreadStates.acquire(this);
			ResourceHolder=NextThreadStates.thread;
			//System.out.println("Thread "+NextThreadStates.thread.getName()+" will running ");
			return  NextThreadStates.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if(WaitQueue.isEmpty()) return null;
			ThreadState NxtThread= WaitQueue.getFirst();
			for(ThreadState Thread : WaitQueue){
				if(Thread.getEffectivePriority()>NxtThread.getEffectivePriority())
					NxtThread=Thread;
			}
			return NxtThread;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		
		public Queue<ThreadState> getWaitQueue() {
			return WaitQueue;
		}

		public boolean transferPriority;
		private LinkedList<ThreadState> WaitQueue = new LinkedList<ThreadState>();
		private KThread ResourceHolder=null;

	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
			this.WaitTime=Machine.timer().getTime();
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
//			int effectivePriority=priority;
//			ThreadState InThread=null;
//			for(PriorityQueue PriQue:Queues){
//				if(PriQue.transferPriority==false) continue;
//				if(PriQue.WaitQueue.isEmpty()) continue;
//				InThread=null;
//				InThread=PriQue.WaitQueue.peek();
//				effectivePriority=Math.max(effectivePriority, InThread.priority);
//			}
			if(effectivePriority<priority) effectivePriority=priority;
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			if(getEffectivePriority()>getThreadState(waitQueue.ResourceHolder).getEffectivePriority()&&waitQueue.transferPriority)
				getThreadState(waitQueue.ResourceHolder).setPriority(getEffectivePriority());
			waitQueue.WaitQueue.add(this);
			if(Queues.contains(waitQueue)==false)
				Queues.add(waitQueue);
			//updateQueuesStates();
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			waitQueue.WaitQueue.remove(this);
			if(Queues.contains(waitQueue)==false)
				Queues.add(waitQueue);
			//updateQueuesStates();
		}
		
		public long getWaitTime(){
			return WaitTime;
		}
		public void updateQueuesStates(){
			boolean del=false;
			for(PriorityQueue MyQueue:Queues){
				if(MyQueue==null) break;
				del=false;
				if(MyQueue.WaitQueue.contains(this)){
					MyQueue.WaitQueue.remove(this);
					del=true;
				}
				for(ThreadState MyStates:MyQueue.WaitQueue){
					int ecfPro=MyStates.getEffectivePriority();
					if(MyStates.priority!=ecfPro){
						ThreadState newThreadState=MyStates;
						MyQueue.WaitQueue.remove(MyStates);
						MyStates.setPriority(ecfPro);
						MyQueue.WaitQueue.add(newThreadState);
					}
					
				}
				if(del) MyQueue.WaitQueue.add(this);
 			}
			
		}
		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		protected long WaitTime;
		/*
		 * recording all WaitQueues of the associated KThread
		 */
		private LinkedList<PriorityQueue> Queues = new LinkedList<PriorityQueue>();
	}
	
}
