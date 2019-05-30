/**
 * 
 */
package Bsnmp;

/**
 * @author Laptop
 *
 */
public class TestMonitor
{
    static final String BD_ADDRESS = "127.0.0.1";
    static final String BOLETAZO_ADDRESS = "25.6.157.184";

    /**
     * Mandar a llamar las intancias necesarias para el monitoreo de los componentes
     * existentes.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        int cpuAlertPer = 80;
        int diskAlertPer = 80;

        Thread monitor = new Monitor("Base de datos", BOLETAZO_ADDRESS, 80, 80);
        monitor.start();
        // monitor = new Monitor("Boletazo server", BOLETAZO_ADDRESS, 80, 80);
        // monitor.start();
    }

}
