package sos;

import java.util.*;

/**
 * This class simulates a simple, sharable write-only device.  
 *
 * @see Sim
 * @see CPU
 * @see SOS
 * @see Device
 */
@SuppressWarnings("unused")
public class KeyboardDevice implements Device
{
    private int m_id = -545;           // the OS assigned device ID

    /**
     * getId
     *
     * @return the device id of this device
     */
    public int getId()
    {
        return m_id;
    }
    
    /**
     * setId
     *
     * sets the device id of this device
     *
     * @param id the new id
     */
    public void setId(int id)
    {
        m_id = id;
    }
    
    /**
     * isSharable
     *
     * This device can be used simultaneously by multiple processes
     *
     * @return true
     */
    public boolean isSharable()
    {
        return false;
    }
    
    /**
     * isAvailable
     *
     * this device is available if no requests are currently being processed
     */
    public boolean isAvailable()
    {
        return true;
    }
    
    /**
     * isReadable
     *
     * @return whether this device can be read from (true/false)
     */
    public boolean isReadable()
    {
        return true;
    }
     
    
    /**
     * isWriteable
     *
     * @return whether this device can be written to (true/false)
     */
    public boolean isWriteable()
    {
        return false;
    }
     
    /**
     * read
     *
     * returns a random number between 1 and 10 inclusive
     * 
     */
    public int read(int addr /*not used*/)
    {
    	
    	int value = (int) (Math.random()*10 + 1);
    	
    	return value;
    }//read
    
    
    /**
     * write
     *
     * not implemented
     */
    public void write(int addr /*not used*/, int data)
    {
        return;
    }
    
};//class ConsoleDevice
