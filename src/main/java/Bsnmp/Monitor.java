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
    final Address DB_ADDRESS = GenericAddress.parse("udp:25.6.157.184/161");

    final int ALERT_CPU_USAGE = 80;
    final int ALERT_MEMORY_USAGE = 80;
    TransportMapping tm; // = new DefaultUdpTransportMapping();
    Snmp sn;

    public Monitor() throws IOException
    {
        try
        {
            tm = new DefaultUdpTransportMapping();
        }
        catch (SocketException ex)
        {

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

    public void secuencialMonitoring() throws IOException, InterruptedException, NullPointerException
    {
        boolean alive = true;
        while (alive)
        {
            // GENERIC METHOD OIDs
            // Windows and Generics hrProcessorLoad 1.3.6.1.2.1.25.3.3.1.2
            // Windows and Generics hrSWRunPerfMem 1.3.6.1.2.1.25.5.1.1.2
            // Windows and Generics N.Memory.SNMP.HrStorage 1.3.6.1.2.1.25.2.3.1.2
            String sysInfo = getAsString(new OID(".1.3.6.1.4.1.2021.4.5.0"));
            logger.info("Monitoreo hrProcessorLoad :::  " + sysInfo);
            String data = "";
            analyzeData(data);
        }
    }

    public void analyzeData(String data)
    {
        try
        {
            int percentaje = Integer.parseInt(data);
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
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
        Address targetAddress = DB_ADDRESS;
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

}
