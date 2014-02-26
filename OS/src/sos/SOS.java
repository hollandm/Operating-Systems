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


	//======================================================================
	//Member variables
	//----------------------------------------------------------------------

	/**
	 * This flag causes the SOS to print lots of potentially helpful
	 * status messages
	 **/
	public static final boolean m_verbose = true;

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
	 * List of all proceses loaded in ram
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
        m_processes.remove(m_currProcess);
        
        //if no other non-blocked process are avaialble then scheduleNewProcess will not
        //overwrite m_currProcess. We will allow m_currProcess to continue running until a
        //new process becomes avaialble. At that point all referecnes to that process should
        //cease to exist and the garbage collecter can handle the rest
        
        debugPrintln("The process " + m_currProcess.getProcessId() + " has been removed from RAM");
        
        scheduleNewProcess();
    }//removeCurrentProcess

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

        if (m_processes.size() == 0) {
        	System.out.println("No more processes available");
        	System.exit(0);
        }
        
        ProcessControlBlock newProcess = getRandomProcess();
        if (newProcess == null)
        	return;
        
        m_currProcess.save(m_CPU);

        m_currProcess = newProcess;
        
        debugPrintln("The process " + m_currProcess.getProcessId() + " has begun running");
        
        m_currProcess.restore(m_CPU);

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

		//TODO: try to figure out what this *3 came from, can we get rid of it?
//		if(prog.getSize() >= allocSize){
//			allocSize = prog.getSize()*3; //enlarge allocSize to fit program
//		}

		// Check that we have enough space to create another process
		if (allocSize + m_nextLoadPos > m_RAM.getSize()) {
			debugPrintln("Not sufficent space in ram to add another process");
			System.exit(0);
		}
		
		//Save register values of the currently running process to somewhere
		m_currProcess.save(m_CPU);
		
		int stackSize = allocSize - prog.getSize();

		m_CPU.setBASE(m_nextLoadPos); //Set base to arbitrary value (can be changed above)

		m_CPU.setLIM(allocSize); 

		m_nextLoadPos += allocSize;
		
		m_CPU.setPC(m_CPU.getBASE()); 

		m_CPU.setSP(m_CPU.getBASE() + allocSize - stackSize); 

		//load the program into memory so it can execute
		for(int i = 0; i < testProcess.length; i++){
			m_RAM.write(i + m_CPU.getBASE(), testProcess[i]);
		}//for

		ProcessControlBlock newProcess = new ProcessControlBlock(m_nextProcessID);
		++m_nextProcessID;
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


    
    //<method header needed>
    private void syscallYield()
    {
        
    	scheduleNewProcess();
    	
    }//syscallYield

    
	/**
	 * syscallExit
	 * 
	 * Terminates the program
	 */
	public void syscallExit() {
		removeCurrentProcess();
	}


	/**
	 * syscallOutput
	 * 
	 * Pops an item off the stack and prints it to console
	 */
	public void syscallOutput() {
		int output = m_CPU.pop();

		System.out.println("OUTPUT: " + output);
	}


	/**
	 * syscallGetPID
	 * 
	 * Returns the process id of the system
	 */
	public void syscallGetPID() {
		m_CPU.push(m_currProcess.getProcessId());
	}


	/**
	 * syscallCoreDump
	 * 
	 * calls CPU.regDump, prints the top 3 items on the stack, terminates the program
	 * 
	 */
	public void syscallCoreDump() {
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
	public void syscallOpen() {
		int deviceNum = m_CPU.pop();
		DeviceInfo deviceInfo = getDeviceInfo(deviceNum);

		//check that the device exists
		if (deviceInfo == null) {
			m_CPU.push(DEVICE_NOT_FOUND_ERROR);
			return;
		}

		//check that the device is sharable and if not then check that it's not already open
		if (!deviceInfo.getDevice().isSharable() && !deviceInfo.unused()) {
//			m_CPU.push(DEVICE_NOT_SHARABLE_ERROR);
			deviceInfo.addProcess(m_currProcess);
			
			
			//address is left as zero since it doesn't apply to opening a device (I think)
			m_currProcess.block(m_CPU, deviceInfo.getDevice(), SYSCALL_OPEN, 0);
			
			m_CPU.push(SYSTEM_HANDLER_SUCCESS);
			
			scheduleNewProcess();
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
	 * syscallClose
	 * 
	 * Removes the current process from the device whose id is on the stack.
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code.
	 */
	public void syscallClose() {
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
		
		if (blockedProcess != null) {
			blockedProcess.unblock();
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
	public void syscallWrite() {

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
	 * syscallRead
	 * 
	 * pops address and device id off the stack (in that order)
	 * reads from the device
	 * 
	 * If the operation is not successful it will push an error code to the stack,
	 * otherwise it will push a success code
	 */
	public void syscallRead() {

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
         * save
         *
         * saves the current CPU registers into this.registers
         *
         * @param cpu  the CPU object to save the values from
         */
        public void save(CPU cpu)
        {
            int[] regs = cpu.getRegisters();
            this.registers = new int[CPU.NUMREG];
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                this.registers[i] = regs[i];
            }
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
            int[] regs = cpu.getRegisters();
            for(int i = 0; i < CPU.NUMREG; i++)
            {
                regs[i] = this.registers[i];
            }

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
            
            debugPrintln("Process " + m_currProcess.getProcessId() + " has been blocked");
            
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
            blockedForDevice = null;
            blockedForOperation = -1;
            blockedForAddr = -1;
            
        }//block
        
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
         * toString       **DEBUGGING**
         *
         * @return a string representation of this class
         */
        public String toString()
        {
            String result = "Process id " + processId + " ";
            if (isBlocked())
            {
                result = result + "is BLOCKED: ";
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
