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
    //Constants
    //----------------------------------------------------------------------

    //These constants define the system calls this OS can currently handle
    public static final int SYSCALL_EXIT     = 0;    /* exit the current program */
    public static final int SYSCALL_OUTPUT   = 1;    /* outputs a number */
    public static final int SYSCALL_GETPID   = 2;    /* get current process id */
    public static final int SYSCALL_COREDUMP = 9;    /* print process state and exit */
	
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

    //insert method header here
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
     * Interrupt Handlers
     *----------------------------------------------------------------------
     */

    //None yet!
    
    /*======================================================================
     * System Calls
     *----------------------------------------------------------------------
     */
    
    //None yet!
    
    
    //<insert header comment here>
    public void systemCall()
    {
    	
    	int syscall_input = m_CPU.pop();
    	
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
    	m_CPU.push(42);
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
    
    
	@Override
	public void interruptIllegalMemoryAccess(int addr) {
		System.out.println("Illegal Memory Access!");
        System.exit(0);
		
	}

	@Override
	public void interruptDivideByZero() {
		System.out.println("Divide by Zero Error!");
        System.exit(0);
		
	}

	@Override
	public void interruptIllegalInstruction(int[] instr) {
		System.out.println("Illegal Intruction!");
        System.exit(0);
		
	}

    
};//class SOS
