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
   
public class SOS
{
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
        int stackSize = allocSize - testProcess.length;//set the size of the stack in proportion to allocSize
        
        m_CPU.setBASE(base); //Set base to arbitrary value
        
        //Set limit according to allocSize, base and stack size
        m_CPU.setLIM(m_CPU.getBASE() + allocSize); 
        
        m_CPU.setPC(m_CPU.getBASE()); //PC starts at base offset
        
        m_CPU.setSP(m_CPU.getBASE() + allocSize - stackSize); //set stack pointer
        
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
    
};//class SOS
