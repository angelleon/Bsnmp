/**
 * 
 */
package Bsnmp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * @author Laptop
 *
 */
public class TestMonitor
{
    final static Logger logger = LogManager.getLogger(TestMonitor.class);

    static final String BD_ADDRESS = "172.16.9.57";
    static final String BOLETAZO_ADDRESS = "172.16.8.226";
    static final String DEVICE_NAME_BD = "Base de datos";
    static final String DEVICE_NAME_BOLETAZO = "Boletazo server";

    static final int memAlertPer = 80;
    static final int diskAlertPer = 80;

    /**
     * Mandar a llamar las intancias necesarias para el monitoreo de los componentes
     * existentes.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        Thread monitorBoletazo;
        Thread monitorBD;
        try
        {
            monitorBoletazo = new Monitor(DEVICE_NAME_BOLETAZO, BOLETAZO_ADDRESS, memAlertPer, diskAlertPer);
            monitorBD = new Monitor(DEVICE_NAME_BD, BD_ADDRESS, memAlertPer, diskAlertPer);
            monitorBoletazo.start();
            monitorBD.start();
        }
        catch (SAXException | ParserConfigurationException | IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("Error - Mala lectura de correos de distribucion XML");
        }
        // monitor = new Monitor("Boletazo server", BOLETAZO_ADDRESS, 80, 80);
        // monitor.start();
    }

}
