package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

public class WaitThreadQueue  {

	
	public void waitForAccess(Object thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    WTQueue.add(thread);
	}

	
	public Object nextWaitThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
		if (WTQueue.isEmpty())
		return null;

	    return (Object) WTQueue.removeFirst();
	}


	public void acquire(Object thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		       
	    Lib.assertTrue(WTQueue.isEmpty());
	}

	/**
	 * Print out the contents of the queue.
	 */
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	}
	public Iterator getIterator(){
		return WTQueue.iterator();
	}
	public int getSize(){
		return WTQueue.size();
	}
	public boolean isEmpty(){
		return WTQueue.isEmpty();
	}
	private LinkedList<Object> WTQueue = new LinkedList<Object>();

}
