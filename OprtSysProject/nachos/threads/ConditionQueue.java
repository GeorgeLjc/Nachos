package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class ConditionQueue extends ThreadQueue {

	@Override
	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    ConQueue.add(thread);
	}

	@Override
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
		if (ConQueue.isEmpty())
		return null;

	    return (KThread) ConQueue.removeFirst();
	}

	@Override
	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    Lib.assertTrue(ConQueue.isEmpty());
	}

	/**
	 * Print out the contents of the queue.
	 */
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    for (Iterator i=ConQueue.iterator(); i.hasNext(); )
		System.out.print((KThread) i.next() + " ");
	}
	public boolean isEmpty(){
		return ConQueue.isEmpty();
	}
	private LinkedList<KThread> ConQueue = new LinkedList<KThread>();

}
