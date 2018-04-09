package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	if(condLock==null){
    		condLock=new Lock();
    		SpeakCond=new  Condition2(condLock);
    		ListenCond=new Condition2(condLock);
    		SpeakEnable=new Semaphore(1);
    	}
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	boolean initStatus=Machine.interrupt().disable();
    	condLock.acquire();
    	if(ListenNum==0){
    		SpeakNum++;
    		SpeakCond.sleep();
    		Buffer.add(word);
    		System.out.println("1-Speaker puts : "+word);
    		ListenCond.wake();
    		SpeakNum--;
    	}else{
    		Buffer.add(word);
    		System.out.println("2-Speaker puts : "+word);
    		ListenCond.wake();
    	}
    	SpeakCond.sleep();//give enough time for listener to read buffer
    	condLock.release();
    	Machine.interrupt().restore(initStatus);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	boolean initStatus=Machine.interrupt().disable();
    	condLock.acquire();
    	if(SpeakNum==0){
    		ListenNum++;
    		ListenCond.sleep();
    		ListenNum--;
    	}else{
    		SpeakCond.wake();
    		ListenCond.sleep();
    	}
    	SpeakCond.wake();//acknowledge the speaker that read completed
    	condLock.release();
    	Machine.interrupt().restore(initStatus);
    	return Buffer.poll();
    }
    
    private static Condition2 SpeakCond=null;
    private static Condition2 ListenCond=null;
    private static Lock condLock=null;
    private static Semaphore SpeakEnable;
    LinkedList<Integer> Buffer=new LinkedList<Integer>();
    private static int ListenNum=0;
    private static int SpeakNum=0;
}
