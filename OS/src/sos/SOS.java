package sos;

import java.util.*;

import javax.swing.text.html.MinimalHTMLWriter;

/**
 * This class contains the simulated operating system (SOS).  Realistically it
 * would run on the same processor (CPU) that it is managing but instead it uses
 * the real-world processor in order to allow a focus on the essentials of
 * operating system design using a high level programming language.
 *
 * @authors harber14, hollandm15
 */

public class SOS implements CPU.TrapHandler
{

	//======================================================================
	// Constants
	//----------------------------------------------------------------------

	public static final int SYSCALL_OPEN    = 3;    /* access a device */
	public static final int SYSCALL_CLOSE   = 4;    /* release a device */
	public static final int SYSCALL_READ    = 5;    /* get input from device */
	public static final int SYSCALL_WRITE   = 6;    /* send output to device */

	//These constants define the system calls this OS can currently handle
	public static final int SYSCALL_EXIT     = 0;    /* exit the current program */
	public static final int SYSCALL_OUTPUT   = 1;    /* outputs a number */
	public static final int SYSCALL_GETPID   = 2;    /* get current process id */
	public static final int SYSCALL_COREDUMP = 9;    /* print process state and exit */


	public static final int SYSCALL_EXEC    = 7;    /* spawn a new process */
	public static final int SYSCALL_YIELD   = 8;    /* yield the CPU to another process */

	// These constants define the error codes used by the system handers encounter errors

	public static final int SYSTEM_HANDLER_SUCCESS = 0;
	public static final int DEVICE_NOT_FOUND_ERROR = -1;
	public static final int DEVICE_NOT_SHARABLE_ERROR = -2;
	public static final int DEVICE_ALREADY_OPEN_ERROR = -3;
	public static final int DEVICE_NOT_OPEN_ERROR = -4;
	public static final int DEVICE_READ_ONLY_ERROR = -5;
	public static final int DEVICE_WRITE_ONLY_ERROR = -6;

	/**This process is used as the idle process' id*/
	public static final int IDLE_PROC_ID    = 999;  


	//======================================================================
	//Member variables
	//----------------------------------------------------------------------

	/**
	 * This flag causes the SOS to print lots of potentially helpful
	 * status messages
	 **/
	public static final boolean m_verbose = false;

	/**
	 * The CPU the operating system is managing.
	 **/
	private CPU m_CPU = null;

	/**
	 * The RAM attached to the CPU.
	 **/
	private RAM m_RAM = null;


	private ProcessControlBlock m_currProcess = null;

	/**
	 * The device attached to the CPU.
	 */
	private Vector<DeviceInfo> m_devices = null;

	/**
	 * All parsed asm files
	 */
	private Vector<Program> m_programs = new Vector<Program>();

	/**
	 * Next location in memory to place a process
	 */
	private int m_nextLoadPos = 0;

	/**
	 * ID of next process created
	 */
	private int m_nextProcessID = 1001;

	/**
	 * List of all processes loaded in ram
	 */
	private Vector<ProcessControlBlock> m_processes = new Vector<ProcessControlBlock>();


	/*======================================================================
	 * Constructors & Debugging
	 *----------------------------------------------------------------------
	 */

	/**
	 * The constructor does nothing special
	 */
	public SOS(CPU c, RAM r)
	{
		//Init member list
		m_CPU = c;
		m_RAM = r;
		m_CPU.registerTrapHandler(this);

		m_currProcess = new ProcessControlBlock(m_nextProcessID);
		//		++m_nextProcessID;

		m_devices = new Vector<SOS.DeviceInfo>();
	}//SOS ctor

	/**
	 * Does a System.out.print as long as m_verbose is true
	 **/
	public static void debugPrint(String s)
	{
		if (m_verbose)
		{
			System.out.print(s);
		}
	}

	/**
	 * Does a System.out.println as long as m_verbose is true
	 **/
	public static void debugPrintln(String s)
	{
		if (m_verbose)
		{
			System.out.println(s);
		}
	}

	/*======================================================================
	 * Memory Block Management Methods
	 *----------------------------------------------------------------------
	 */

	//None yet!

	/*======================================================================
	 * Device Management Methods
	 *----------------------------------------------------------------------
	 */

	//None yet!

	/*======================================================================
	 * Process Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * createIdleProcess
	 *
	 * creates a one instruction process that immediately exits.  This is used
	 * to buy time until device I/O completes and unblocks a legitimate
	 * process.
	 *
	 */
	public void createIdleProcess()
	{
		//		debugPrintln("Creating Idle Processs");
		int progArr[] = { 0, 0, 0, 0,   //SET r0=0
				0, 0, 0, 0,   //SET r0=0 (repeated instruction to account for vagaries in student implementation of the CPU class)
				10, 0, 0, 0,   //PUSH r0
				15, 0, 0, 0 }; //TRAP

		//Initialize the starting position for this program
		int baseAddr = m_nextLoadPos;

		//Load the program into RAM
		for(int i = 0; i < progArr.length; i++)
		{
			m_RAM.write(baseAddr + i, progArr[i]);
		}

		//Save the register info from the current process (if there is one)
		if (m_currProcess != null)
		{
			m_currProcess.save(m_CPU);
		}

		//Set the appropriate registers
		m_CPU.setPC(baseAddr);
		m_CPU.setSP(baseAddr + progArr.length + 10);
		m_CPU.setBASE(baseAddr);
		m_CPU.setLIM(baseAddr + progArr.length + 20);

		//Save the relevant info as a new entry in m_processes
		m_currProcess = new ProcessControlBlock(IDLE_PROC_ID);  
		m_processes.add(m_currProcess);

	}//createIdleProcess

	/**
	 * printProcessTable      **DEBUGGING**
	 *
	 * prints all the processes in the process table
	 */
	private void printProcessTable()
	{
		debugPrintln("");
		debugPrintln("Process Table (" + m_processes.size() + " processes)");
		debugPrintln("======================================================================");
		for(ProcessControlBlock pi : m_processes)
		{
			debugPrintln("    " + pi);
		}//for
		debugPrintln("----------------------------------------------------------------------");

	}//printProcessTable

	/**
	 * removeCurrentProcess
	 * 
	 * removes the current process from m_processes and randomly selects a new process
	 */
	public void removeCurrentProcess()
	{
		debugPrintln("The process " + m_currProcess.getProcessId() + " has been removed from RAM");

		m_processes.remove(m_currProcess);

		//if no other non-blocked process are available then scheduleNewProcess will not
		//overwrite m_currProcess. We will allow m_currProcess to continue running until a
		//new process becomes available. At that point all references to that process should
		//cease to exist and the garbage collector can handle the rest


		scheduleNewProcess();
	}//removeCurrentProcess

	/**
	 * getProcess
	 * 
	 * selects a process to run in a manner to minimize starvation and process switching
	 * 
	 */
	ProcessControlBlock getProcess() {

		//This method worked really great
		//		m_CPU.addTicks(-1000);

		//Abusing the system to allow a process to avoid running into block processes
		//		if (m_currProcess.isBlocked()) {
		//			try {
		//				Thread.sleep(5);
		//			} catch (InterruptedException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//		}
		//		return getRandomProcess();


		//Always let the idle process finish to avoid extra switching
		if (m_currProcess.getProcessId() == SOS.IDLE_PROC_ID) {
			if (m_processes.contains(m_currProcess)) 
				return m_currProcess;
		}

		ProcessControlBlock selected = null;


		//idle process tick minimization loop
		for (int i = 0; i < m_processes.capacity(); ++i) {

			double minValue = Double.MAX_VALUE;
			for (ProcessControlBlock s : m_processes) {
				if (!s.isBlocked() && s.avgStarve < minValue) {
					selected = s;
					minValue = s.avgStarve + 300;
				}
			}
		}

		return selected;

		//Bogo-sort m_processes, take first non-blocked process.

		//		ArrayList<ProcessControlBlock> l;
		//		
		//		bogoLoop:
		//		while (true) {
		//			
		//			l = new ArrayList<>();
		//			for (ProcessControlBlock pcb : m_processes)
		//				bogoRandomInsert(l, pcb);
		//			
		//			for (int i = 1; i < l.size(); ++i) {
		//				ProcessControlBlock currNode = l.get(i-1);
		//				ProcessControlBlock nextNode = l.get(i);
		//				
		//				if (currNode.avgStarve > nextNode.avgStarve)
		//					continue bogoLoop;
		//			}
		//			
		//			break bogoLoop;
		//		}
		//		
		//		if (l.get(0).avgStarve > m_currProcess.avgStarve + 300) {
		//			return l.get(0);
		//		} else {
		//			return m_currProcess;
		//		}

	}

	/**
	 * Helper Method for bogo sort
	 * 
	 * @param l - the array list to add things into
	 * @param pcb - the pcb to add to the list
	 */
	public void bogoRandomInsert(ArrayList<ProcessControlBlock> l, ProcessControlBlock pcb) {

		int random = (int)(Math.random()*l.size());

		l.add(random, pcb);

	}

	/**
	 * getRandomProcess
	 *
	 * selects a non-Blocked process at random from the ProcessTable.
	 *
	 * @return a reference to the ProcessControlBlock struct of the selected process
	 * -OR- null if no non-blocked process exists
	 */
	ProcessControlBlock getRandomProcess()
	{
		//Calculate a random offset into the m_processes list
		int offset = ((int)(Math.random() * 2147483647)) % m_processes.size();

		//Iterate until a non-blocked process is found
		ProcessControlBlock newProc = null;
		for(int i = 0; i < m_processes.size(); i++)
		{
			newProc = m_processes.get((i + offset) % m_processes.size());
			if ( ! newProc.isBlocked())
			{
				return newProc;
			}
		}//for

		return null;        // no processes are Ready
	}//getRandomProcess

	/**
	 * scheduleNewProcess
	 * 
	 * Checks if a random non-blocked process is available
	 * if it is available then run it
	 * otherwise if no process are available end the simulation
	 */
	public void scheduleNewProcess()
	{

		printProcessTable();

		if (m_processes.isEmpty()) {
			System.out.println("No more processes available");
			System.exit(0);
		}

		//		ProcessControlBlock newProcess = getRandomProcess();
		ProcessControlBlock newProcess = getProcess();

		debugPrintln("The process " + m_currProcess.getProcessId() + " has been moved to the ready state");

		if  (m_currProcess != newProcess) {
			m_currProcess.save(m_CPU);

			//If their isn't an unblocked process then make an idle process.
			if (newProcess == null) {
				//			debugPrintln("Creating Idle Process");
				createIdleProcess();
				return;
			}



			m_currProcess = newProcess;

			//		debugPrintln("The process " + m_currProcess.getProcessId() + " has begun running");

			m_currProcess.restore(m_CPU);
		}
	}//scheduleNewProcess

	/**
	 * addProgram
	 *
	 * registers a new program with the simulated OS that can be used when the
	 * current process makes an Exec system call.  (Normally the program is
	 * specified by the process via a filename but this is a simulation so the
	 * calling process doesn't actually care what program gets loaded.)
	 *
	 * @param prog  the program to add
	 *
	 */
	public void addProgram(Program prog)
	{
		m_programs.add(prog);
	}//addProgram




	/**
	 * selectBlockedProcess
	 *
	 * select a process to unblock that might be waiting to perform a given
	 * action on a given device.  This is a helper method for system calls
	 * and interrupts that deal with devices.
	 *
	 * @param dev   the Device that the process must be waiting for
	 * @param op    the operation that the process wants to perform on the
	 *              device.  Use the SYSCALL constants for this value.
	 * @param addr  the address the process is reading from.  If the
	 *              operation is a Write or Open then this value can be
	 *              anything
	 *
	 * @return the process to unblock -OR- null if none match the given criteria
	 */
	public ProcessControlBlock selectBlockedProcess(Device dev, int op, int addr)
	{
		ProcessControlBlock selected = null;
		for(ProcessControlBlock pi : m_processes)
		{
			if (pi.isBlockedForDevice(dev, op, addr))
			{
				selected = pi;
				break;
			}
		}//for

		return selected;
	}//selectBlockedProcess

	/*======================================================================
	 * Program Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * createProcess
	 * 
	 * Allocates memory and initializes a process
	 * 
	 * @param prog, the program to that is running the process
	 * @param allocSize, how much memory will this program need
	 */
	public void createProcess(Program prog, int allocSize)
	{

		int testProcess[] = prog.export(); //load parsed process

		//TODO: try to figure out what this came from, can we get rid of it? It seems sketchy
		if(prog.getSize() >= allocSize){
			allocSize = prog.getSize()*3; //enlarge allocSize to fit program
		}

		// Check that we have enough space to create another process
		if (allocSize + m_nextLoadPos > m_RAM.getSize()) {
			debugPrintln("Not sufficent space in ram to add another process");
			System.exit(0);
		}



		//Save register values of the currently running process to somewhere
		m_currProcess.save(m_CPU);

		//Create a new process
		ProcessControlBlock newProcess = new ProcessControlBlock(m_nextProcessID);
		++m_nextProcessID;

		int newBase = m_nextLoadPos;
		m_nextLoadPos += allocSize;

		//initialize registers
		m_CPU.setBASE(newBase); //Set base to arbitrary value (can be changed above)
		m_CPU.setLIM(allocSize); 
		m_CPU.setPC(newBase); 
		m_CPU.setSP(newBase + allocSize - 1); 


		//load the program into memory so it can execute
		for(int i = 0; i < testProcess.length; i++){
			m_RAM.write(i + newBase, testProcess[i]);
		}//for


		m_processes.add(newProcess);
		m_currProcess = newProcess;

		debugPrintln("The process " + m_currProcess.getProcessId() + " has been added into RAM");


	}//createProcess




	/*======================================================================
	 * System Calls
	 *----------------------------------------------------------------------
	 */


	/**
	 * systemCall
	 * 
	 * Whenever trap is called by assembly code we must read an item off
	 *  the stack and then perform an appropriate system call
	 */
	public void systemCall()
	{

		int syscall_input = m_CPU.pop();

		//See method headers for details of these operations

		switch (syscall_input) {
		case SYSCALL_EXIT:
			syscallExit();
			break;
		case SYSCALL_OUTPUT:
			syscallOutput();
			break;
		case SYSCALL_GETPID:
			syscallGetPID();
			break;
		case SYSCALL_COREDUMP:
			syscallCoreDump();
			break;
		case SYSCALL_OPEN:
			syscallOpen();
			break;
		case SYSCALL_READ:
			syscallRead();
			break;
		case SYSCALL_CLOSE:
			syscallClose();
			break;
		case SYSCALL_WRITE:
			syscallWrite();
			break;
		case SYSCALL_YIELD:
			syscallYield();
			break;
		case SYSCALL_EXEC:
			syscallExec();
			break;
		default:
			break;
		}

	}


	/**
	 * syscallExec
	 *
	 * creates a new process.  The program used to create that process is chosen
	 * semi-randomly from all the programs that have been registered with the OS
	 * via {@link #addProgram}.  Limits are put into place to ensure that each
	 * process is run an equal number of times.  If no programs have been
	 * registered then the simulation is aborted with a fatal error.
	 *
	 */
	private void syscallExec()
	{
		//If there is nothing to run, abort.  This should never happen.
		if (m_programs.size() == 0)
		{
			System.err.println("ERROR!  syscallExec has no programs to run.");
			System.exit(-1);
		}

		//find out which program has been called the least and record how many
		//times it has been called
		int leastCallCount = m_programs.get(0).callCount;
		for(Program prog : m_programs)
		{
			if (prog.callCount < leastCallCount)
			{
				leastCallCount = prog.callCount;
			}
		}

		//Create a vector of all programs that have been called the least number
		//of times
		Vector<Program> cands = new Vector<Program>();
		for(Program prog : m_programs)
		{
			cands.add(prog);
		}

		//Select a random program from the candidates list
		Random rand = new Random();
		int pn = rand.nextInt(m_programs.size());
		Program prog = cands.get(pn);

		//Determine the address space size using the default if available.
		//Otherwise, use a multiple of the program size.
		int allocSize = prog.getDefaultAllocSize();
		if (allocSize <= 0)
		{
			allocSize = prog.getSize() * 2;
		}

		//Load the program into RAM
		createProcess(prog, allocSize);

		//Adjust the PC since it's about to be incremented by the CPU
		m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);

	}//syscallExec


	/**
	 * syscallYield
	 * 
	 * Description: puts the current process to sleep (moves it to the ready state)
	 * 
	 */
	private void syscallYield()
	{
		scheduleNewProcess();
	}//syscallYield


	/**
	 * syscallExit
	 * 
	 * Terminates the program
	 */
	private void syscallExit() {
		removeCurrentProcess();
	}


	/**
	 * syscallOutput
	 * 
	 * Pops an item off the stack and prints it to console
	 */
	private void syscallOutput() {
		int output = m_CPU.pop();

		System.out.println("OUTPUT: " + output);
	}


	/**
	 * syscallGetPID
	 * 
	 * Returns the process id of the system
	 */
	private void syscallGetPID() {
		m_CPU.push(m_currProcess.getProcessId());
	}


	/**
	 * syscallCoreDump
	 * 
	 * calls CPU.regDump, prints the top 3 items on the stack, terminates the program
	 * 
	 */
	private void syscallCoreDump() {
		m_CPU.regDump();

		int output = m_CPU.pop();
		System.out.println("OUTPUT: " + output);

		output = m_CPU.pop();
		System.out.println("OUTPUT: " + output);

		output = m_CPU.pop();
		System.out.println("OUTPUT: " + output);

		System.exit(0);
	}

	/**
	 * syscallOpen
	 * 
	 * Assigns the current process to the device whose id is on the stack.
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code.
	 */
	private void syscallOpen() {
		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//check that the device is not already open
		if (deviceInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_ALREADY_OPEN_ERROR);
			return;
		}

		//check that the device is sharable and if not then check that it's not already open
		if (!deviceInfo.getDevice().isSharable() && !deviceInfo.unused()) {
			//			m_CPU.push(DEVICE_NOT_SHARABLE_ERROR);
			deviceInfo.addProcess(m_currProcess);
			debugPrintln("Blocked Process " + m_currProcess.getProcessId() + " on device " + deviceInfo.getId());
			debugPrintln(m_currProcess.toString());
			//address is left as zero since it doesn't apply to opening a device (I think)
			m_currProcess.block(m_CPU, deviceInfo.getDevice(), SYSCALL_OPEN, 0);

			m_CPU.push(SYSTEM_HANDLER_SUCCESS);

			scheduleNewProcess();
			return;
		}


		//		debugPrintln("Process " + m_currProcess.getProcessId() + " opened device " + deviceNum);

		deviceInfo.addProcess(m_currProcess);
		m_CPU.push(SYSTEM_HANDLER_SUCCESS);
	}


	/**
	 * syscallClose
	 * 
	 * Removes the current process from the device whose id is on the stack.
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code.
	 */
	private void syscallClose() {
		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//Check that device is not already open
		if (!deviceInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_NOT_OPEN_ERROR);
			return;
		}

		deviceInfo.removeProcess(m_currProcess);

		ProcessControlBlock blockedProcess = selectBlockedProcess(deviceInfo.getDevice(), SYSCALL_OPEN, 0);
		//Again addr is left as 0 since as far as I can tell it does not apply


		//		debugPrintln("Process " + m_currProcess.getProcessId() + " closed device " + deviceNum);

		if (blockedProcess != null) {
			blockedProcess.unblock();

			//			debugPrintln("Process " + blockedProcess.getProcessId() + " opened device " + deviceNum);
		} else {
			System.out.print("");
		}


		m_CPU.push(SYSTEM_HANDLER_SUCCESS);
	}


	/**
	 * syscallWrite
	 * 
	 * pops data, address, and device id off the stack (in that order)
	 * writes to the device
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code
	 * 
	 */
	private void syscallWrite() {

		int data = m_CPU.pop();
		int addr = m_CPU.pop();

		int deviceNum = m_CPU.pop();
		DeviceInfo devInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (devInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//Called after if  to prevent a method call on a null object
		Device dev = devInfo.getDevice();

		//Check that device is not already open
		if (!devInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_NOT_OPEN_ERROR);
			return;
		}

		//Check that the device is not read only
		if (!dev.isWriteable()) {
			m_CPU.push(DEVICE_READ_ONLY_ERROR);
			return;
		}


		if (!dev.isAvailable()) {
			m_CPU.push(deviceNum);
			m_CPU.push(addr);
			m_CPU.push(data);
			m_CPU.push(SOS.SYSCALL_WRITE);
			m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);
			debugPrintln("Process id " + m_currProcess.getProcessId() + " is now ready");

			scheduleNewProcess();
			return;
		}

		//Block the process
		this.m_currProcess.save(m_CPU);
		this.m_currProcess.block(m_CPU, dev, SOS.SYSCALL_WRITE, addr);

		dev.write(addr, data);

		scheduleNewProcess();

	}

	/**
	 * syscallRead
	 * 
	 * pops address and device id off the stack (in that order)
	 * reads from the device
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code
	 */
	private void syscallRead() {


		int addr = m_CPU.pop();

		int deviceNum = m_CPU.pop();

		DeviceInfo devInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (devInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}
		Device dev = devInfo.getDevice();

		//Check that device is already open
		if (!devInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_NOT_OPEN_ERROR);
			return;
		}

		//Check that the device is not read only
		if (!dev.isReadable()) {
			m_CPU.push(DEVICE_WRITE_ONLY_ERROR);
			return;
		}

		if (!dev.isAvailable()) {
			m_CPU.push(deviceNum);
			m_CPU.push(addr);
			m_CPU.push(SOS.SYSCALL_READ);
			m_CPU.setPC(m_CPU.getPC() - CPU.INSTRSIZE);
			debugPrintln("Process id " + m_currProcess.getProcessId() + " is now ready");
			scheduleNewProcess();
			return;
		}


		//Block the process
		this.m_currProcess.save(m_CPU);
		this.m_currProcess.block(m_CPU, dev, SOS.SYSCALL_READ, addr);

		dev.read(addr);
		scheduleNewProcess();


	}


	/**
	 * getDevice
	 * 
	 * Helper method to get the device from the list of devices based on its device id.
	 * 
	 * @param deviceId
	 * 
	 * @return the device, null if no device found
	 */
	public DeviceInfo getDeviceInfo(int deviceId) {
		for (DeviceInfo dev : m_devices) {
			if (dev.getId() == deviceId) {
				return dev;
			}
		}

		return null;
	}

	/*======================================================================
	 * Interrupt Handlers
	 *----------------------------------------------------------------------
	 */

	/**
	 * interruptIllegalMemoryAccess
	 * 
	 * When the program tries to get data from outside its designated space close the program.
	 * 
	 * @param addr
	 */
	@Override
	public void interruptIllegalMemoryAccess(int addr) {
		System.out.println("Illegal Memory Access of addr: " + addr);
		System.exit(0);

	}

	/**
	 * interruptDivideByZero
	 * 
	 * When the program tries to divide by zero close the program.
	 * 
	 */
	@Override
	public void interruptDivideByZero() {
		System.out.println("Divide by Zero Error!");
		System.exit(0);

	}

	/**
	 * interruptIllegalInstruction
	 * 
	 * When the program runs into an illegal instruction close the program.
	 * 
	 */
	@Override
	public void interruptIllegalInstruction(int[] instr) {
		System.out.println("Illegal Intruction!");
		System.exit(0);

	}

	/**
	 * interruptIOReadComplete
	 * 
	 * Description: when a device finishes a read instruction it calls this function to interrupt
	 * 		the currently running process to hand off the read data to the relevant process
	 * 
	 * @param devID - the id of the device that was being written to
	 * @param addr - the address we wrote to
	 * @param data - the data returned from the read
	 */
	@Override
	public void interruptIOReadComplete(int devID, int addr, int data) {

		DeviceInfo devInfo = getDeviceInfo(devID);

		ProcessControlBlock blocked = selectBlockedProcess(devInfo.getDevice(), SYSCALL_READ, addr);

		if (blocked == null) {
			System.out.println("Null blocked process, interruptIOReadComplete");
			System.exit(0);
		}

		//		debugPrintln("The process " + m_currProcess.getProcessId() + " has recived an interupt from device " + devID);
		blocked.unblock();

		//Push the data we received from the read to the reading processes stack
		blocked.push(data);

		//Push a successful system call indicator to the reading processes stack
		blocked.push(SOS.SYSTEM_HANDLER_SUCCESS);

		//		blocked.setRegisterValue(CPU.SP, blockedSP);

	}

	/**
	 * interruptIOWriteComplete
	 * 
	 * Description: when a device finishes a write intruction it calls this function to interupt
	 * 		the currently running process to notify the relavent process that the operation completed
	 * 
	 * @param devID - the id of the device that was being written to
	 * @param addr - the address we wrote to
	 */
	@Override
	public void interruptIOWriteComplete(int devID, int addr) {
		DeviceInfo devInfo = getDeviceInfo(devID);

		ProcessControlBlock blocked = selectBlockedProcess(devInfo.getDevice(), SYSCALL_WRITE, addr);


		System.out.println("Device Procs Size: "+devInfo.procs.size());
		if (blocked == null) {
			System.out.println("Null blocked process, interruptIOWriteComplete");
			System.exit(0);
		}

		blocked.push(SOS.SYSTEM_HANDLER_SUCCESS);

		blocked.unblock();
		//		debugPrintln("The process " + m_currProcess.getProcessId() + " has recived an interupt from device " + devID);
	}

	/**
	 * 
	 */
	public void interruptClock() {
		scheduleNewProcess();
	}


	//======================================================================
	// Inner Classes
	//----------------------------------------------------------------------

	/**
	 * class ProcessControlBlock
	 *
	 * This class contains information about a currently active process.
	 */
	private class ProcessControlBlock
	{
		/**
		 * the time it takes to load and save registers, specified as a number
		 * of CPU ticks
		 */
		private static final int SAVE_LOAD_TIME = 30;

		/**
		 * Used to store the system time when a process is moved to the Ready
		 * state.
		 */
		private int lastReadyTime = -1;

		/**
		 * Used to store the number of times this process has been in the ready
		 * state
		 */
		private int numReady = 0;

		/**
		 * Used to store the maximum starve time experienced by this process
		 */
		private int maxStarve = -1;

		/**
		 * Used to store the average starve time for this process
		 */
		private double avgStarve = 0;

		/**
		 * The number of ticks that this 
		 */
		private int numStarvationTicks = 0;

		/**
		 * These are the process' current registers.  If the process is in the
		 * "running" state then these are out of date
		 */
		private int[] registers = null;

		/**
		 * If this process is blocked a reference to the Device is stored here
		 */
		private Device blockedForDevice = null;

		/**
		 * If this process is blocked a reference to the type of I/O operation
		 * is stored here (use the SYSCALL constants defined in SOS)
		 */
		private int blockedForOperation = -1;

		/**
		 * If this process is blocked reading from a device, the requested
		 * address is stored here.
		 */
		private int blockedForAddr = -1;

		/**
		 * a unique id for this process
		 */
		private int processId = 0;

		/**
		 * constructor
		 *
		 * @param pid        a process id for the process.  The caller is
		 *                   responsible for making sure it is unique.
		 */
		public ProcessControlBlock(int pid)
		{
			this.processId = pid;
		}

		/**
		 * @return the current process' id
		 */
		public int getProcessId()
		{
			return this.processId;
		}

		/**
		 * @return the last time this process was put in the Ready state
		 */
		public long getLastReadyTime()
		{
			return lastReadyTime;
		}


		/**
		 * save
		 *
		 * saves the current CPU registers into this.registers
		 *
		 * @param cpu  the CPU object to save the values from
		 */
		public void save(CPU cpu)
		{
			//A context switch is expensive.  We simulate that here by 
			//adding ticks to m_CPU
			m_CPU.addTicks(SAVE_LOAD_TIME);

			//Save the registers
			int[] regs = cpu.getRegisters();
			this.registers = new int[CPU.NUMREG];
			for(int i = 0; i < CPU.NUMREG; i++)
			{
				this.registers[i] = regs[i];
			}

			//Assuming this method is being called because the process is moving
			//out of the Running state, record the current system time for
			//calculating starve times for this process.  If this method is
			//being called for a Block, we'll adjust lastReadyTime in the
			//unblock method.
			numReady++;
			lastReadyTime = m_CPU.getTicks();

		}//save

		/**
		 * restore
		 *
		 * restores the saved values in this.registers to the current CPU's
		 * registers
		 *
		 * @param cpu  the CPU object to restore the values to
		 */
		public void restore(CPU cpu)
		{
			//A context switch is expensive.  We simulate that here by 
			//adding ticks to m_CPU
			m_CPU.addTicks(SAVE_LOAD_TIME);

			//Restore the register values
			int[] regs = cpu.getRegisters();
			for(int i = 0; i < CPU.NUMREG; i++)
			{
				regs[i] = this.registers[i];
			}

			//Record the starve time statistics
			int starveTime = m_CPU.getTicks() - lastReadyTime;
			if (starveTime > maxStarve)
			{
				maxStarve = starveTime;
			}
			double d_numReady = (double)numReady;
			avgStarve = avgStarve * (d_numReady - 1.0) / d_numReady;
			avgStarve = avgStarve + (starveTime * (1.0 / d_numReady));
		}//restore

		/**
		 * block
		 *
		 * blocks the current process to wait for I/O.  The caller is
		 * responsible for calling {@link CPU#scheduleNewProcess}
		 * after calling this method.
		 *
		 * @param cpu   the CPU that the process is running on
		 * @param dev   the Device that the process must wait for
		 * @param op    the operation that the process is performing on the
		 *              device.  Use the SYSCALL constants for this value.
		 * @param addr  the address the process is reading from (for SYSCALL_READ)
		 * 
		 */
		public void block(CPU cpu, Device dev, int op, int addr)
		{
			blockedForDevice = dev;
			blockedForOperation = op;
			blockedForAddr = addr;

			debugPrintln("Process " + m_currProcess.getProcessId() + " has been blocked while waiting for " + dev.getId());

		}//block

		/**
		 * unblock
		 *
		 * moves this process from the Blocked (waiting) state to the Ready
		 * state. 
		 *
		 */
		public void unblock()
		{
			//Reset the info about the block
			blockedForDevice = null;
			blockedForOperation = -1;
			blockedForAddr = -1;

			//Assuming this method is being called because the process is moving
			//from the Blocked state to the Ready state, record the current
			//system time for calculating starve times for this process.
			lastReadyTime = m_CPU.getTicks();

		}//unblock

		/**
		 * isBlocked
		 *
		 * @return true if the process is blocked
		 */
		public boolean isBlocked()
		{
			return (blockedForDevice != null);
		}//isBlocked

		/**
		 * isBlockedForDevice
		 *
		 * Checks to see if the process is blocked for the given device,
		 * operation and address.  If the operation is not an open, the given
		 * address is ignored.
		 *
		 * @param dev   check to see if the process is waiting for this device
		 * @param op    check to see if the process is waiting for this operation
		 * @param addr  check to see if the process is reading from this address
		 *
		 * @return true if the process is blocked by the given parameters
		 */
		public boolean isBlockedForDevice(Device dev, int op, int addr)
		{
			if ( (blockedForDevice == dev) && (blockedForOperation == op) )
			{
				if (op == SYSCALL_OPEN)
				{
					return true;
				}

				if (addr == blockedForAddr)
				{
					return true;
				}
			}//if

			return false;
		}//isBlockedForDevice

		/**
		 * getRegisterValue
		 *
		 * Retrieves the value of a process' register that is stored in this
		 * object (this.registers).
		 * 
		 * @param idx the index of the register to retrieve.  Use the constants
		 *            in the CPU class
		 * @return one of the register values stored in in this object or -999
		 *         if an invalid index is given 
		 */
		public int getRegisterValue(int idx)
		{
			if ((idx < 0) || (idx >= CPU.NUMREG))
			{
				return -999;    // invalid index
			}

			return this.registers[idx];
		}//getRegisterValue

		/**
		 * setRegisterValue
		 *
		 * Sets the value of a process' register that is stored in this
		 * object (this.registers).  
		 * 
		 * @param idx the index of the register to set.  Use the constants
		 *            in the CPU class.  If an invalid index is given, this
		 *            method does nothing.
		 * @param val the value to set the register to
		 */
		public void setRegisterValue(int idx, int val)
		{
			if ((idx < 0) || (idx >= CPU.NUMREG))
			{
				return;    // invalid index
			}

			this.registers[idx] = val;
		}//setRegisterValue



		/**
		 * toString       **DEBUGGING**
		 *
		 * @return a string representation of this class
		 */
		public String toString()
		{
			String result = "Process id " + processId + " ";
			if (isBlocked())
			{
				result = result + "is BLOCKED for ";
				if (blockedForOperation == SYSCALL_OPEN)
				{
					result = result + "OPEN";
				}
				else if (blockedForOperation == SYSCALL_READ)
				{
					result = result + "READ @" + blockedForAddr;
				}
				else if (blockedForOperation == SYSCALL_WRITE)
				{
					result = result + "WRITE @" + blockedForAddr;
				}
				else  
				{
					result = result + "unknown reason!";
				}
				for(DeviceInfo di : m_devices)
				{
					if (di.getDevice() == blockedForDevice)
					{
						result = result + " on device #" + di.getId();
						break;
					}
				}
				result = result + ": ";
			}
			else if (this == m_currProcess)
			{
				result = result + "is RUNNING: ";
			}
			else
			{
				result = result + "is READY: ";
			}

			if (registers == null)
			{
				result = result + "<never saved>";
				return result;
			}

			for(int i = 0; i < CPU.NUMGENREG; i++)
			{
				result = result + ("r" + i + "=" + registers[i] + " ");
			}//for
			result = result + ("PC=" + registers[CPU.PC] + " ");
			result = result + ("SP=" + registers[CPU.SP] + " ");
			result = result + ("BASE=" + registers[CPU.BASE] + " ");
			result = result + ("LIM=" + registers[CPU.LIM] + " ");

			return result;
		}//toString

		/**
		 * compareTo              
		 *
		 * compares this to another ProcessControlBlock object based on the BASE addr
		 * register.  Read about Java's Collections class for info on
		 * how this method can be quite useful to you.
		 */
		public int compareTo(ProcessControlBlock pi)
		{
			return this.registers[CPU.BASE] - pi.registers[CPU.BASE];
		}

		/**
		 * push
		 * 
		 * Pushes data to the stack of this process
		 * 
		 * @param data
		 */
		public void push(int data) {
			int sp = getRegisterValue(CPU.SP) - CPU.STACKITEMSIZE;
			setRegisterValue(CPU.SP, sp);
			m_RAM.write(sp, data);
		}

		/**
		 * 
		 * pop
		 * 
		 * Pops data from this processes stack
		 * 
		 * @return
		 */
		public int pop() {
			int sp = getRegisterValue(CPU.SP) + CPU.STACKITEMSIZE;
			setRegisterValue(CPU.SP, sp);
			return m_RAM.read(sp+CPU.STACKITEMSIZE);

		}

	}//class ProcessControlBlock

	/**
	 * class DeviceInfo
	 *
	 * This class contains information about a device that is currently
	 * registered with the system.
	 */
	private class DeviceInfo
	{
		/** every device has a unique id */
		private int id;
		/** a reference to the device driver for this device */
		private Device device;
		/** a list of processes that have opened this device */
		private Vector<ProcessControlBlock> procs;

		/**
		 * constructor
		 *
		 * @param d          a reference to the device driver for this device
		 * @param initID     the id for this device.  The caller is responsible
		 *                   for guaranteeing that this is a unique id.
		 */
		public DeviceInfo(Device d, int initID)
		{
			this.id = initID;
			this.device = d;
			d.setId(initID);
			this.procs = new Vector<ProcessControlBlock>();
		}

		/** @return the device's id */
		public int getId()
		{
			return this.id;
		}

		/** @return this device's driver */
		public Device getDevice()
		{
			return this.device;
		}

		/** Register a new process as having opened this device */
		public void addProcess(ProcessControlBlock pi)
		{
			procs.add(pi);
		}

		/** Register a process as having closed this device */
		public void removeProcess(ProcessControlBlock pi)
		{
			procs.remove(pi);
		}

		/** Does the given process currently have this device opened? */
		public boolean containsProcess(ProcessControlBlock pi)
		{
			return procs.contains(pi);
		}

		/** Is this device currently not opened by any process? */
		public boolean unused()
		{
			return procs.size() == 0;
		}


	}//class DeviceInfo


	/*======================================================================
	 * Device Management Methods
	 *----------------------------------------------------------------------
	 */

	/**
	 * registerDevice
	 *
	 * adds a new device to the list of devices managed by the OS
	 *
	 * @param dev     the device driver
	 * @param id      the id to assign to this device
	 * 
	 */
	public void registerDevice(Device dev, int id)
	{
		m_devices.add(new DeviceInfo(dev, id));
	}//registerDevice





};//class SOS
