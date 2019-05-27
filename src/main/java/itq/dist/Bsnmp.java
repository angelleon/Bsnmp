package itq.dist;

import com.jayway.snmpblogg.SimpleSnmpClient;
import org.snmp4j.smi.OID;

import java.io.IOException;

public class Bsnmp
{
    public static void main(String[] args)
    {
        try
        {
            SimpleSnmpClient client = new SimpleSnmpClient("udp:127.0.0.1/161");
            String sysDesc = client.getAsString(new OID(".1.3.6.1.2.1.1.1.0"));
            System.out.println(sysDesc);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
