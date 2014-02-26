package sos;

import java.util.*;

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


	// These constants define the error codes used by the system handers encounter errors

	public static final int SYSTEM_HANDLER_SUCCESS = 0;
	public static final int DEVICE_NOT_FOUND_ERROR = -1;
	public static final int DEVICE_NOT_SHARABLE_ERROR = -2;
	public static final int DEVICE_ALREADY_OPEN_ERROR = -3;
	public static final int DEVICE_NOT_OPEN_ERROR = -4;
	public static final int DEVICE_READ_ONLY_ERROR = -5;
	public static final int DEVICE_WRITE_ONLY_ERROR = -6;


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
	 * An arbitrary variable for the base value for memory.
	 **/
	private int base = 21;


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

		m_currProcess = new ProcessControlBlock(42);

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

	//None yet!

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

		if(prog.getSize() >= allocSize){
			allocSize = prog.getSize()*3; //enlarge allocSize to fit program
		}

		int stackSize = allocSize - prog.getSize();

		m_CPU.setBASE(base); //Set base to arbitrary value (can be changed above)

		m_CPU.setLIM(allocSize); 

		m_CPU.setPC(m_CPU.getBASE()); 

		m_CPU.setSP(m_CPU.getBASE() + allocSize - stackSize); 

		//load the program into memory so it can execute
		for(int i = 0; i < testProcess.length; i++){
			m_RAM.write(i + m_CPU.getBASE(), testProcess[i]);
		}//for

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
			handleSyscallExit();
			break;
		case SYSCALL_OUTPUT:
			handleSyscallOutput();
			break;
		case SYSCALL_GETPID:
			handleSyscallGetPID();
			break;
		case SYSCALL_COREDUMP:
			handleSyscallCoreDump();
			break;
		case SYSCALL_OPEN:
			handleSyscallOpen();
			break;
		case SYSCALL_READ:
			handleSyscallRead();
			break;
		case SYSCALL_CLOSE:
			handleSyscallClose();
			break;
		case SYSCALL_WRITE:
			handleSyscallWrite();
			break;

		default:
			break;
		}

	}

	/**
	 * handleSyscallExit
	 * 
	 * Terminates the program
	 */
	public void handleSyscallExit() {
		System.exit(0);
	}


	/**
	 * handleSyscallOutput
	 * 
	 * Pops an item off the stack and prints it to console
	 */
	public void handleSyscallOutput() {
		int output = m_CPU.pop();

		System.out.println("OUTPUT: " + output);
	}


	/**
	 * handleSyscallGetPID
	 * 
	 * Not yet implemented, pushes 42 to the stack
	 */
	public void handleSyscallGetPID() {
		m_CPU.push(m_currProcess.getProcessId());
	}


	/**
	 * handleSyscallCoreDump
	 * 
	 * calls CPU.regDump, prints the top 3 items on the stack, terminates the program
	 * 
	 */
	public void handleSyscallCoreDump() {
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
	 * handleSyscallOpen
	 * 
	 * Assigns the current process to the device whose id is on the stack.
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code.
	 */
	public void handleSyscallOpen() {
		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//check that the device is sharable and if not then check that it's not already open
		if (!deviceInfo.getDevice().isSharable() && !deviceInfo.unused()) {
			m_CPU.push(DEVICE_NOT_SHARABLE_ERROR);
			return;
		}

		//check that the device is not already open
		if (deviceInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_ALREADY_OPEN_ERROR);
			return;
		}



		deviceInfo.addProcess(m_currProcess);
		m_CPU.push(SYSTEM_HANDLER_SUCCESS);
	}


	/**
	 * handleSyscallClose
	 * 
	 * Removes the current process from the device whose id is on the stack.
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code.
	 */
	public void handleSyscallClose() {
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
		m_CPU.push(SYSTEM_HANDLER_SUCCESS);
	}


	/**
	 * handleSyscallWrite
	 * 
	 * pops data, address, and device id off the stack (in that order)
	 * writes to the device
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code
	 * 
	 */
	public void handleSyscallWrite() {

		int data = m_CPU.pop();
		int addr = m_CPU.pop();

		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//Called after if  to prevent a method call on a null object
		Device device = deviceInfo.getDevice();

		//Check that device is not already open
		if (!deviceInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_NOT_OPEN_ERROR);
			return;
		}

		//Check that the device is not read only
		if (!device.isWriteable()) {
			m_CPU.push(DEVICE_READ_ONLY_ERROR);
			return;
		}

		//TODO: Ensure that device is not writing out of bounds?
		device.write(addr, data);
		m_CPU.push(SYSTEM_HANDLER_SUCCESS);

	}

	/**
	 * handleSyscallRead
	 * 
	 * pops address and device id off the stack (in that order)
	 * reads from the device
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code
	 */
	public void handleSyscallRead() {

		int addr = m_CPU.pop();

		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//Called after if  to prevent a method call on a null object
		Device device = deviceInfo.getDevice();

		//Check that device is not already open
		if (!deviceInfo.containsProcess(m_currProcess)) {
			m_CPU.push(DEVICE_NOT_OPEN_ERROR);
			return;
		}

		//Check that the device is not read only
		if (!device.isReadable()) {
			m_CPU.push(DEVICE_WRITE_ONLY_ERROR);
			return;
		}

		int data = device.read(addr);

		//value should be pushed before you push the success/error code
		m_CPU.push(data);
		m_CPU.push(SYSTEM_HANDLER_SUCCESS);

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
		System.out.println("Illegal Memory Access!");
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
