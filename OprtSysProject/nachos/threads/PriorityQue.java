package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.ThreadState;
public class PriorityQue {
	public static void  static_selftest(){
//		
//		Scheduler Schd=new PriorityScheduler();
//		
//		KThread PriTest1=new KThread(new PriorityQueTest(1)).setName("DefaultPriorityThread");
//		KThread PriTest2=new KThread(new PriorityQueTest(2)).setName("MaximumPriorityThread");
//		KThread PriTest3=new KThread(new PriorityQueTest(3)).setName("MinimumPriorityThread");
//		
//		ThreadQueue Que1=Schd.newThreadQueue(false);
//		ThreadQueue Que2=Schd.newThreadQueue(false);
//		ThreadQueue Que3=Schd.newThreadQueue(false);
//		
//		boolean initstatus = Machine.interrupt().disable();
//		ThreadedKernel.scheduler.setPriority(PriTest1,PriorityScheduler.priorityDefault);
//		ThreadedKernel.scheduler.setPriority(PriTest2,PriorityScheduler.priorityMaximum);
//		ThreadedKernel.scheduler.setPriority(PriTest3, PriorityScheduler.priorityMinimum);
//		
//		
//		Que1.waitForAccess(PriTest1);
//		Que1.waitForAccess(PriTest3);
//		
//		Que2.waitForAccess(PriTest1);
//		Que2.waitForAccess(PriTest3);
//		
//		Que3.acquire(PriTest3);
//		Que3.waitForAccess(PriTest1);
//		Que3.waitForAccess(PriTest2);
//		
//		Machine.interrupt().setStatus(initstatus);
//		PriTest1.fork();
//		PriTest2.fork();
//		PriTest3.fork();
//		
//		KThread.yield();
//		
		
		System.out.println("##########********PriorityScheduler Test starts********##########");
		ThreadQueue ResQue = ThreadedKernel.scheduler.newThreadQueue(true);
		KThread kthread1 = new KThread().setName("KThread 1"), kthread2 = new KThread().setName("KThread 2");
		
		boolean status = Machine.interrupt().disable();
		
		ResQue.waitForAccess(kthread1);
	
		
		ThreadedKernel.scheduler.setPriority(kthread1, 6);
		
		System.out.println("EffectivePriority of KThread1 is:"+new PriorityScheduler().getEffectivePriority(kthread1));
		System.out.println("EffectivePriority of KThread2 is:"+new PriorityScheduler().getEffectivePriority(kthread2));
		
		ResQue.acquire(kthread2);
		
		System.out.println("KThread2 holds resources");
		System.out.println("EffectivePriority of KThread2 is:"+new PriorityScheduler().getEffectivePriority(kthread2));
		
		
		KThread kthread3 = new KThread().setName("KThread 3");
		KThread kthread4 = new KThread().setName("KThread 4");
		
		ThreadedKernel.scheduler.setPriority(kthread4, 7);
		ThreadedKernel.scheduler.setPriority(kthread3, 7);
		System.out.println("EffectivePriority of KThread3 is:"+new PriorityScheduler().getEffectivePriority(kthread3));
		
		/*
		 * KThread 3 and KThread 4 share the same priority but thread 4 was added into the queue
		 * earlier than KThread 3 
		*/
		ResQue.waitForAccess(kthread4);
		ResQue.waitForAccess(kthread3);
		
		System.out.println("KThread2 holds resources");
		System.out.println("EffectivePriority of KThread2 is:"+new PriorityScheduler().getEffectivePriority(kthread2));
		
		/*
		 * pop the top two KThread (KThread 4 ,KThread 3) from ResQue
		 */
		System.out.println(ResQue.nextThread().getName()+" pop out ");
		System.out.println(ResQue.nextThread().getName()+" pop out ");
		//System.out.println(tq1.nextThread().getName());//if do so the Priority of KThread 1 will become 1
		
		System.out.println("KThread2 finish");
		System.out.println("EffectivePriority of KThread2 is:"+new PriorityScheduler().getEffectivePriority(kthread2));
		
		Machine.interrupt().restore(status);

		
	}
	
	public static void  dynamic_selftest(){
		Lock recource =new Lock();
		KThread Pritest1 =new KThread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				recource.acquire();
				KThread.yield();
				for(int i=1;i<=5;++i){
					System.out.println("**** "+"Pritest 1 run "+i+" times ");
				}
				recource.release();
			}

		}).setName("Pritest 1");
		Pritest1.fork();
		KThread.yield();
	}
	
	
}
