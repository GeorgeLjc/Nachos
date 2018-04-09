package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class JoinQueue extends ThreadQueue {

	@Override
	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    JoinQueue.add(thread);
	}

	@Override
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    if (JoinQueue.isEmpty())
		return null;

	    return (KThread) JoinQueue.removeFirst();
	}

	@Override
	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    Lib.assertTrue(JoinQueue.isEmpty());
	}

	/**
	 * Print out the contents of the queue.
	 */
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    for (Iterator i=JoinQueue.iterator(); i.hasNext(); )
		System.out.print((KThread) i.next() + " ");
	}
	private LinkedList<KThread> JoinQueue = new LinkedList<KThread>();

}
