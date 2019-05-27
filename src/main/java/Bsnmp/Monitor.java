package Bsnmp;

import java.io.IOException;
import java.lang.Thread;
import java.net.SocketException;

import org.snmp4j.*;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class Monitor extends Thread
{
    Address DB_ADDRESS = GenericAddress.parse("udp:127.0.0.1/161");
    TransportMapping tm; // = new DefaultUdpTransportMapping();
    Snmp sn;

    public Monitor()
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
            sn.listen();
        }
        catch (IOException ex)
        {

        }
    }
}
