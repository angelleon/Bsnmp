package Bsnmp;

import java.io.IOException;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class Monitor extends Thread
{
    final static Logger logger = LogManager.getLogger(Monitor.class);
    final String ADMON_MAIL = "fuentesamaury@hotmail.com";
    final String IP_MAIL = "localhost";
    final String PORT_MAIL = "465";

    private Address address = GenericAddress.parse("udp:127.0.0.1/161");
    private int onCheckObjects = 3; // maximos objetos escaneados
    private int alertCpu = 80; // DEFAULT ALERT GRADE ON %
    private int alertDisk = 80; // DEFAULT ALERT GRADE ON %
    private TransportMapping tm; // = new DefaultUdpTransportMapping();
    private Snmp sn;

    private enum estado {
        ESTABLE,
        ALERTA,
        CRITICO,
        ERROR,
    }

    private enum errorCode {
        UDP_ERROR_INSTANCE,
        BAD_DATA_READER,
        BAD_DATA_INCOMING,
    }

    /***
     * Inicia el monitoreo de la instancia seleccionada proporcionando el porcentaje
     * de alerta para establecer estado critico en los componentes del sistema
     * monitoreado.
     * 
     * @param alertCpu
     * @param alertDisk
     * @throws IOException
     */
    public Monitor(String address, int alertCpu, int alertDisk) throws IOException
    {
        this.alertCpu = alertCpu;
        this.alertDisk = alertDisk;
        this.address = GenericAddress.parse("upd:" + address + "/161");
        try
        {
            tm = new DefaultUdpTransportMapping();
        }
        catch (SocketException ex)
        {
            ex.printStackTrace();
            logger.error("Error - Falla en protocolo UDP ");
        }
        sn = new Snmp(tm);
    }

    /***
     * Inicia el monitoreo de la instancia seleccionada con un nivel de alerta de
     * 80% para iniciar estado critico en los componentes del sistema monitoreado
     * 
     * @param address
     * @throws IOException
     */
    public Monitor(String address) throws IOException
    {
        this.address = GenericAddress.parse("upd:" + address + "/161");
        try
        {
            tm = new DefaultUdpTransportMapping();
        }
        catch (SocketException ex)
        {
            ex.printStackTrace();
            logger.error("Error - Falla en protocolo UDP ");
        }
        sn = new Snmp(tm);
    }

    @Override
    public void run()
    {
        try
        {
            tm.listen();
            secuencialMonitoring();
        }

        catch (InterruptedException e)
        {
            e.printStackTrace();
            logger.error("Error - Interrupcion pronta en HILO");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            logger.error("Error IO - GENERIC ERROR");
        }
    }

    /***
     * Inicia el monitoreo sobre los componentes del sistema.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws NullPointerException
     */
    public void secuencialMonitoring() throws IOException, InterruptedException, NullPointerException
    {
        boolean alive = true;
        while (alive)
        {
            // GENERIC METHOD OIDs
            // Windows and Generics hrProcessorLoad 1.3.6.1.2.1.25.3.3.1.2
            // Windows and Generics hrSWRunPerfMem 1.3.6.1.2.1.25.5.1.1.2
            // Windows and Generics N.Memory.SNMP.HrStorage 1.3.6.1.2.1.25.2.3.1.2
            String sysInfo3 = getAsString(new OID(".1.3.6.1.4.1.2021.4.5"));
            logger.info("Monitoreo SpaceFreeOnDisk :::  " + sysInfo3);
            String sysInfo2 = getAsString(new OID(".1.3.6.1.4.1.2021.4.5"));
            logger.info("Monitoreo SpaceFreeOnDisk :::  " + sysInfo2);
            String sysInfo = getAsString(new OID(".1.3.6.1.4.1.2021.4.11.0"));
            logger.info("Monitoreo MemoryFree Kb :::  " + sysInfo);
            Thread.sleep(5000);
            String data1 = "";
            String data2 = "";
            analyzeData(data1, data2);
        }
    }

    /***
     * Toma desiciones en base a los porcentajes recibidos para catalogarlos como
     * criticos o estables en caso de ser critico, mandara solicitud al cliente para
     * dar un aviso al administrador.
     * 
     * @param dataMemory
     * @param diskMemory
     */
    public void analyzeData(String dataMemory, String diskMemory)
    {
        int[] analyzerArray = new int[3]; // Storage for data analyzer
        try
        {
            analyzerArray[0] = Integer.parseInt("1");
            analyzerArray[1] = Integer.parseInt("0");
            analyzerArray[2] = Integer.parseInt("2");
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            logger.error("Error - BadData Format on snmp");
        }

        for (int i = 0; i < onCheckObjects; i++)
        {
            switch (i)
            {
            case 0:
                if (analyzerArray[0] < (alertCpu - 100))
                {

                }
                break;
            case 1:
                if (analyzerArray[1] < (alertDisk - 100))
                {

                }
                break;
            case 2:
                if (analyzerArray[2] < (alertCpu - 100))
                {

                }
                break;
            default:
                break;
            }
        }
    }

    public String getAsString(OID oid) throws IOException
    {
        ResponseEvent event = get(new OID[] {oid});
        return event.getResponse().get(0).getVariable().toString();
    }

    public ResponseEvent get(OID oids[]) throws IOException
    {
        PDU pdu = new PDU();
        for (OID oid : oids)
        {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        // logger.debug("PDU: " + pdu);
        ResponseEvent event = sn.send(pdu, getTarget(), null);
        if (event != null) { return event; }
        throw new RuntimeException("GET timed out");
    }

    private Target getTarget()
    {
        Address targetAddress = address;
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

}
