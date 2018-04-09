package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		LastProcessLock.acquire();
		PID = NextPID++;
		runningProcessNum++;
		LastProcessLock.release();
		// 文件描述符表0 和 1 分配给标准输入和标准输出 (即屏幕)
		fileTable[0] = UserKernel.console.openForReading();
		FileReference.referenceFile(fileTable[0].getName());
		fileTable[1] = UserKernel.console.openForWriting();
		FileReference.referenceFile(fileTable[1].getName());
		waitingToJoin = new Condition(joinLock);

		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length && memoryAccessLock != null);
		if (!validVirtualMemAddress(vaddr)) {
			return 0;
		} else {
			Collection<MemoryAccess> memoryAccesses = createMemoryAccesses(
					vaddr, data, offset, length, AccessType.READ);

			int bytesRead = 0, temp;

			memoryAccessLock.acquire();
			for (MemoryAccess ma : memoryAccesses) {
				temp = ma.executeAccess();

				if (temp == 0)
					break;
				else
					bytesRead += temp;
			}
			memoryAccessLock.release();
			return bytesRead;
		}
		//
		// byte[] memory = Machine.processor().getMemory();
		//
		// // for now, just assume that virtual addresses equal physical
		// addresses
		// if (vaddr < 0 || vaddr >= memory.length)
		// return 0;
		//
		// int amount = Math.min(length, memory.length - vaddr);
		// System.arraycopy(memory, vaddr, data, offset, amount);
		//
		// return amount;
	}

	private Collection<MemoryAccess> createMemoryAccesses(int vaddr,
		byte[] data, int offset, int length, AccessType accessType) {
		LinkedList<MemoryAccess> returnList = new LinkedList<MemoryAccess>();

		while (length > 0) {
			int vpn = Processor.pageFromAddress(vaddr);// calculate the virtual page number

			int potentialPageAccess = Processor.pageSize
					- Processor.offsetFromAddress(vaddr);
			int accessSize = Math.min(potentialPageAccess,length);

			returnList.add(new MemoryAccess(accessType, data, vpn, offset,
					Processor.offsetFromAddress(vaddr), accessSize));
			length -= accessSize;
			vaddr += accessSize;
			offset += accessSize;
		}

		return returnList;
	}
	
	protected class MemoryAccess {
		protected MemoryAccess(AccessType at, byte[] d, int _vpn, int dStart, int pStart, int len) {
			accessType = at;
			data = d;
			vpn = _vpn;
			dataStart = dStart;
			pageStart = pStart;
			length = len;
		}
	
		/*
		 * execute the MemoryAccess that had been asked
		 * @return the number of bytes successfully write or -1 if fail 
		 */
		public int executeAccess() {
			if (translationEntry == null)
				translationEntry = pageTable[vpn];
			if (translationEntry.valid) {
				if (accessType == AccessType.READ) {// Do a read
					System.arraycopy(Machine.processor().getMemory(), pageStart
							+ (Processor.pageSize * translationEntry.ppn),
							data, dataStart, length);
					translationEntry.used = true;
					return length;
				} else if (!translationEntry.readOnly
						&& accessType == AccessType.WRITE) {// FIXME: If this
															// last part
															// necessary?
					System.arraycopy(data, dataStart, Machine.processor()
							.getMemory(), pageStart
							+ (Processor.pageSize * translationEntry.ppn),
							length);
					translationEntry.used = translationEntry.dirty = true;
					return length;
				}
			}

			return 0;
		}

		protected byte[] data;// 一个索引到我们应该写入的数组
		protected AccessType accessType;// 哪个访问应该发生
		protected TranslationEntry translationEntry;// 和可以存取的恰当的页相一致的转化入口
		protected int dataStart;// 访问数组的界限
		protected int pageStart;// 访问页的界限
		protected int length;// 存取的长度，数组和页长度一样
		protected int vpn;// 页需要的VPN
	}
	
	protected static enum AccessType {//枚举类决定什么数据访问
		READ, WRITE
	};


	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length && memoryAccessLock != null);
		if (!validVirtualMemAddress(vaddr)) {
			return 0;
		} else {
			Collection<MemoryAccess> memoryAccesses = createMemoryAccesses(
					vaddr, data, offset, length, AccessType.WRITE);

			int bytesWritten = 0, temp;
			memoryAccessLock.acquire();
			for (MemoryAccess ma : memoryAccesses) {
				temp = ma.executeAccess();
				if (temp == 0)
					break;
				else
					bytesWritten += temp;
			}
			memoryAccessLock.release();

			return bytesWritten;
		}

		//		Lib.assertTrue(offset >= 0 && length >= 0
		//				&& offset + length <= data.length);
		//
		//		byte[] memory = Machine.processor().getMemory();
		//
		//		// for now, just assume that virtual addresses equal physical addresses
		//		if (vaddr < 0 || vaddr >= memory.length)
		//			return 0;
		//
		//		int amount = Math.min(length, memory.length - vaddr);
		//		System.arraycopy(data, offset, memory, vaddr, amount);
		//
		//		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += (4 + argv[i].length + 1);
		}
		if (argsSize > pageSize) {
			coff.close();
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 *
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			return false;
		}
		pageTable = ((UserKernel) Kernel.kernel).acquirePages(numPages);
		for (int i = 0; i < pageTable.length; i++)
			pageTable[i].vpn = i;//vpn虚拟页数

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, pageTable[vpn].ppn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		((UserKernel)Kernel.kernel).releasePages(pageTable);
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}
	
	/*
	 * track the file referred in the system
	 */
	protected static class FileReference {
		int references;
		boolean delete;

		
		public static boolean referenceFile(String fileName) {
			FileReference ref = updateFileReference(fileName);
			boolean canReference = !ref.delete;
			if (canReference)
				ref.references++;
			globalFileReferencesLock.release();
			return canReference;
		}

		/*
		 * @ return : 0 success , -1 fail
		 */
		public static int unreferenceFile(String fileName) {
			FileReference ref = updateFileReference(fileName);
			ref.references--;
			Lib.assertTrue(ref.references >= 0);
			int ret = removeIfNecessary(fileName, ref);
			globalFileReferencesLock.release();
			return ret;
		}

	
		
		public static int deleteFile(String fileName) {
			FileReference ref = updateFileReference(fileName);
			ref.delete = true;
			int ret = removeIfNecessary(fileName, ref);
			globalFileReferencesLock.release();
			return ret;
		}
		/*
		 * no process referred the file, then remove if from the system
		 */
		private static int removeIfNecessary(String fileName, FileReference ref) {
			if (ref.references <= 0) {
				globalFileReferences.remove(fileName);
				if (ref.delete == true) {
					if (!UserKernel.fileSystem.remove(fileName)) return -1;
				}
			}
			return 0;
		}

	
		private static FileReference updateFileReference(String fileName) {
			globalFileReferencesLock.acquire();
			FileReference ref = globalFileReferences.get(fileName);
			if (ref == null) {
				ref = new FileReference();
				globalFileReferences.put(fileName, ref);
			}

			return ref;
		}
		

	
		private static HashMap<String, FileReference> globalFileReferences = new HashMap<String, FileReference> ();
		private static Lock globalFileReferencesLock = new Lock();
		

	}
	/*
	 * define the child process of the program
	 */
	private static class ChildProcess {
		public Integer returnValue;
		public UserProcess process;

		ChildProcess(UserProcess child) {
			process = child;
			returnValue = null;
		}
	}


	/*
	 * check whether the VPN is valid
	 */
	protected boolean validVirtualMemAddress(int vaddr) {
		int vpn = Processor.pageFromAddress(vaddr);//获得虚拟页号,get the virtual page number
		return vpn < numPages && vpn >= 0;
	}
	/*
	 * get the first unused FileDescriptor in fileTable
	 */
	protected int getFileDescriptor() {
		for (int i = 0; i < fileTable.length; i++) {
			if (fileTable[i] == null)
				return i;
		}
		return -1;
	}

	/*
	 * check whether the File Descriptor is valid
	 */
	private boolean validFileDescriptor(int fileDescpt) {
		// In range?
		if (fileDescpt < 0 || fileDescpt >= fileTable.length)
			return false;
		// Table entry valid?
		return fileTable[fileDescpt] != null;
	}
	
	private int terminate() {
		handleExit(null);
		return -1;
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	/*
	 *read data from file to memory 
	 */
	private int handleRead(int fileDescpt,int memoryAddress,int ReadBytes){//文件描述符, 写入的内存地址, 读取的字节数.
		if(!validVirtualMemAddress(memoryAddress)) return terminate();
		if(!validFileDescriptor(fileDescpt)) return terminate();
		byte buffer[] = new byte[ReadBytes];
		int bytesRead = fileTable[fileDescpt].read(buffer, 0, ReadBytes);
		if (bytesRead == -1)// Read failed
			return -1;
		int bytesWritten = writeVirtualMemory(memoryAddress, buffer, 0, bytesRead);//写入内存

		if (bytesWritten != bytesRead)
			return -1;

		return bytesRead;//return the bytes read from file

	}

	/*
	 * Attempt to write up to count bytes from buffer to the file or stream
	 * referred to by fileDescriptor.
	 */
	private int handleWrite(int fileDescpt, int memoryAddress, int WirteBytes) {

		if (!validVirtualMemAddress(memoryAddress)) return terminate();;
		if (!validFileDescriptor(fileDescpt))	return terminate();;

		byte buffer[] = new byte[WirteBytes];
		int bytesRead = readVirtualMemory(memoryAddress, buffer);// 访问内存, 得到要写入文件的内容
		int bytesWritten = fileTable[fileDescpt].write(buffer, 0, bytesRead);// 写入内存
		return bytesWritten;// return bytes write into file
	}
	
	/*
	 * Attempt to open the named file and return a file descriptor.
	 */
	private int openFile(int fileNameMemAddress, boolean create) {
		if (!validVirtualMemAddress(fileNameMemAddress)) return terminate();

		// get a FileDeascrptor
		int fileDescrpt = getFileDescriptor();
		if (fileDescrpt == -1)
			return -1;
		// read the FileName through ReadVirtualMemoryString
		String fileName = readVirtualMemoryString(fileNameMemAddress,
				MaxLength);

		// add a new fileReference to the file
		if (!FileReference.referenceFile(fileName)) return -1; 

		// open the file in system
		OpenFile file = UserKernel.fileSystem.open(fileName, create);
		if (file == null) {
			//delete the reference create above
			FileReference.unreferenceFile(fileName);
			return -1;
		}

		// add the file into fileTable
		fileTable[fileDescrpt] = file;

		return fileDescrpt;
	}
	/*
	 * Attempt to open the named file and return a file descriptor.
	 * @ open and not create while the file is not exist
	 */
	private int handleOpen(int fileNameMemAddress){
		return openFile(fileNameMemAddress, false);
	}
	/*
	 * Attempt to open the named file and return a file descriptor.
	 * @ open and create while the file is not exist
	 */
	private int handleCreate(int fileNamePtr) {
		return openFile(fileNamePtr, true);
	}

	/*
	 * Close a file descriptor, so that it no longer refers to any file or
	 * stream and may be reused.
	 */
	private int handleClose(int fileDescpt) {
		if (!validFileDescriptor(fileDescpt)) return terminate();;
		String fileName = fileTable[fileDescpt].getName();//从本地描述符中得到文件对象
		fileTable[fileDescpt].close();//关闭文件
		fileTable[fileDescpt] = null;	// 从本地描述符表中移出文件对象
		//移出文件名的引用
		return FileReference.unreferenceFile(fileName);
	}

	/*
	 * Delete a file from the file system. If no processes have the file open,
	 * the file is deleted immediately and the space it was using is made
	 * available for reuse.
	 * 
	 * @fileNameMemAddress  the MemoryAddress of FileName
	 */
	private int handleUnlink(int fileNameMemAddress) {
		if (!validVirtualMemAddress(fileNameMemAddress)) return terminate();
		String fileName = readVirtualMemoryString(fileNameMemAddress, MaxLength);
		System.out.println("unlinked");
		return FileReference.deleteFile(fileName);
	}
	
	/*
	 * Execute the program stored in the specified file, with the specified
	 * arguments, in a new child process. The child process has a new unique
	 * process ID, and starts with stdin opened as file descriptor 0, and stdout
	 * opened as file descriptor 1.
	 */
	private int handleExec(int fileNameMemAddress, int argumentNum, int argumentsMemAddress) {

		if (!validVirtualMemAddress(fileNameMemAddress) || !validVirtualMemAddress(argumentsMemAddress))
			return terminate();

		// 读虚拟内存获得文件名
		String fileName = readVirtualMemoryString(fileNameMemAddress,
				MaxLength);
		if (fileName == null || !fileName.endsWith(".coff"))
			return -1;
		String arguments[] = new String[argumentNum];

		int argvLen = argumentNum * 4;
		byte argvArray[] = new byte[argvLen];
		if (argvLen != readVirtualMemory(argumentsMemAddress, argvArray)) {
			return -1;
		}
		for (int i = 0; i < argumentNum; i++) {
			int pointer = Lib.bytesToInt(argvArray, i * 4);
			if (!validVirtualMemAddress(pointer))
				return -1;

			// 用 readVirtualMemoryString 依次读出每个参数
			arguments[i] = readVirtualMemoryString(pointer, MaxLength);
		}
		// 创建子进程
		UserProcess newChild = newUserProcess();
		newChild.parent = this;

		children.put(newChild.PID, new ChildProcess(newChild));
		// 执行子进程
		newChild.execute(fileName, arguments);
		return newChild.PID;
	}
	/*
	 * notify the parent thread that its child process is finish
	 */
	protected void notifyChildExitStatus(int childPID, Integer childStatus) {
		ChildProcess child = children.get(childPID);
		if (child == null)
			return;
		
		child.process = null;
	    child.returnValue = childStatus;
	}

	/*
	 * Terminate the current process immediately. Any open file descriptors
	 * belonging to the process are closed. Any children of the process no
	 * longer have a parent process.
	 */
	private int handleExit(Integer status) {
		joinLock.acquire();
		// this program is going to finish and notify its parent process 
		if (parent != null)
			parent.notifyChildExitStatus(PID, status);
		// set child process's parent process null
		for (ChildProcess child : children.values())
			if (child.process != null)
				child.process.parent=null;
		children = null;

		// close all the openfile of the program and release them
		for (int fileDesc = 0; fileDesc < fileTable.length; fileDesc++)
			if (validFileDescriptor(fileDesc))
				handleClose(fileDesc);

		// release Virtual Memory
		unloadSections();

		exited = true;
		waitingToJoin.wakeAll();
		joinLock.release();

		// if the number of running process equals zero then exit
		LastProcessLock.acquire();
		if (--runningProcessNum == 0)
			Kernel.kernel.terminate();
		LastProcessLock.release();

		// Thread finish
		KThread.finish();

		return 0;
	}
	
	/*
	 * Suspend execution of the current process until the child process
	 * specified by the processID argument has exited. If the child has already
	 * exited by the time of the call, returns immediately. When the current
	 * process resumes, it disowns the child process, so that join() cannot be
	 * used on that process again.
	 */
	private int handleJoin(int pid, int statusMemAddress) {
		if (!validVirtualMemAddress(statusMemAddress))
			return terminate();

		ChildProcess child = children.get(pid);

		if (child == null)
			return -1;

		if (child.process != null)
			child.process.joinProcess();
		//the child process is finish and remove it
		children.remove(pid);

		if (child.returnValue == null)
			return 0;

		//restore the return value of child process
		writeVirtualMemory(statusMemAddress, Lib.bytesFromInt(child.returnValue));

		
		return 1;
	}
	
	private void joinProcess() {
		joinLock.acquire();
		while (!exited)
			waitingToJoin.sleep();
		joinLock.release();
	}


	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	//	System.out.println(syscall);
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			handleExit(a0);
			return 0;
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}
	
	/** the process id*/
	protected int PID;
	/** the PID of next process*/
	protected static int NextPID=0;
	/** the parent process of the program*/
	protected UserProcess parent;
	/** the child process of the program*/
	private HashMap<Integer, ChildProcess> children = new HashMap<Integer, ChildProcess> ();
	/** The MaxLength of filename , SystemCall Arguments etc.*/
	private static final int MaxLength=256;
	/** whether this program exit */
	private boolean exited = false;
	/** Join Condition*/
	private Lock joinLock = new Lock();
	private Condition waitingToJoin;
	/** notes whether the process is the last process*/
	private static Lock LastProcessLock=new Lock();
	/** the lock of the access to the memory*/
	private Lock memoryAccessLock = new Lock();
	/** the number of running process*/
	private static int runningProcessNum=0;
	/** The program being run by this process. */
	protected Coff coff;
	/** files that this program opened , single program can open no more than 16 files*/
	protected OpenFile[] fileTable=new OpenFile[16];
	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
}
