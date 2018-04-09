package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {

	}
	public LotteryThread getLotteryThread(KThread thread){
		//System.out.println("thread name : "+thread.getName());
		if(thread.LotteryScheduling==null)
			thread.LotteryScheduling=new LotteryThread(thread);
		return (LotteryThread)thread.LotteryScheduling;
	}
	public void setTickets(KThread thread, int tickets) {
		boolean initstatus = Machine.interrupt().disable();
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(tickets>0);
		getLotteryThread(thread).setTickets(tickets);
		Machine.interrupt().restore(initstatus);
	}
	public static void LotterySelfTest(){
		LotteryScheduler lottery = new LotteryScheduler();
		Lock lotteryLock =new Lock();
		KThread testthread1 =new KThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				lotteryLock.acquire();
				System.out.println("lotterytestthread-1 sleep ");
				KThread.yield();
				for(int i=0;i<5;++i){
					System.out.println("*** lotterytestthread-1 runs : "+i+" times");
				}
				lotteryLock.release();
			}
		}).setName("LotterTestThread1");
		lottery.setTickets(testthread1, 5);
		testthread1.fork();
		System.out.println("mainthread sleep - before fork thread-2");
		KThread.yield();
		System.out.println("mainthread back - before fork thread-2");
		KThread testthread2 = new KThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				lotteryLock.acquire();
				for (int i = 0; i < 5; ++i) {
					System.out.println("*** lotterytestthread-2 runs : " + i
							+ " times");
				}
				lotteryLock.release();
			}
		}).setName("LotteryTestThread2");
		lottery.setTickets(testthread2,10);
		testthread2.fork();
		System.out.println("mainthread sleep - after fork thread-2");
		KThread.yield();
		System.out.println("mainthread back - after fork thread-2");
	}
	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param transferTickets
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferTickets) {
		// implement me
		// return null;
		return new LotteryQueue(transferTickets);

	}
	protected class LotteryQueue extends ThreadQueue{

		LotteryQueue(boolean transferTickets) {
			this.transferTickets = transferTickets;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getLotteryThread(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getLotteryThread(thread).acquire(this);
			ResourceHolder=thread;
			
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if(WaitQueue.isEmpty())
				return null;
			LotteryThread NextThreadStates = pickNextThread();
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
		protected LotteryThread pickNextThread() {
			// implement me
			if(WaitQueue.isEmpty()) return null;
			LotteryThread NxtThread= WaitQueue.getFirst();
			//calculate current tickets's sum
			int currentTickets=0;
			//random ticket
			int randomTicket=0;
			//the sum of tickets in this queue
			int sumTickets=0;
			System.out.print("Threads in WaitQueues : ");
			for(LotteryThread thread: WaitQueue){
				System.out.print(thread.thread.getName()+":"+thread.getEffectiveTickets()+",");
				sumTickets+=thread.getEffectiveTickets();
			}
			System.out.println();
			randomTicket=new Random().nextInt(sumTickets)+1;
			System.out.println("random ticket number is : "+randomTicket+", sum tickets : "+sumTickets);
			for(LotteryThread Thread : WaitQueue){
				currentTickets+=Thread.getEffectiveTickets();
				if(currentTickets>=randomTicket){
					NxtThread=Thread;
					break;
				}
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
		
		public Queue<LotteryThread> getWaitQueue() {
			return WaitQueue;
		}
		public boolean transferTickets;
		private LinkedList<LotteryThread> WaitQueue = new LinkedList<LotteryThread>();
		private KThread ResourceHolder=null;
		
	}
	protected class LotteryThread{
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public LotteryThread(KThread thread) {
			this.thread = thread;

			addTickets(defaultTickets);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getTickets() {
			return tickets;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectiveTickets() {
			// implement me
			if(effectiveTicket<tickets) effectiveTicket=tickets;
			return effectiveTicket;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void addTickets(int tickets) {

			this.tickets+= tickets;

			// implement me
		}
		public void setTickets(int tickets){
			Lib.assertTrue(tickets>0);
			this.tickets=tickets;
		}
		
		public void waitForAccess(LotteryQueue waitQueue) {
			// implement me
			if(waitQueue.ResourceHolder==null) ;
			else if(getEffectiveTickets()>getLotteryThread(waitQueue.ResourceHolder).getEffectiveTickets()&&waitQueue.transferTickets)
				getLotteryThread(waitQueue.ResourceHolder).addTickets(getEffectiveTickets());
			waitQueue.WaitQueue.add(this);
//			if(Queues.contains(waitQueue)==false)
//				Queues.add(waitQueue);
			//updateQueuesStates();
		}

		
		public void acquire(LotteryQueue waitQueue) {
			// implement me
			waitQueue.WaitQueue.remove(this);
//			if(Queues.contains(waitQueue)==false)
//				Queues.add(waitQueue);
			//updateQueuesStates();
		}
		
		
		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int tickets=0;
		protected int effectiveTicket=0;
		
	}
	protected final static int minmumTickets=1;
	protected final static int defaultTickets=5;
	protected final static int maxmumTickets=Integer.MAX_VALUE;

}
