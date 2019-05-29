/**
 * 
 */
package Bsnmp;

import java.io.IOException;

/**
 * @author Laptop
 *
 */
public class TestMonitor
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            Thread monitor = new Monitor();
            monitor.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Error IOException");
        }
    }

}
