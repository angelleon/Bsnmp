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
            String sysDesc = client.getAsString(new OID(".1.3.6.1.2.1.2.2.1.2.2"));
            System.out.println(sysDesc);

            // Percentages of user CPU time (ssCpuUser)
            String ssCpuUser = client.getAsString(new OID(".1.3.6.1.2.1.2.2.1.10"));
            System.out.println(ssCpuUser);

            // Percentages of system CPU time (ssCpuSystem)
            String ssCpuSystem = client.getAsString(new OID(".1.3.6.1.2.1.2.2.1.14"));
            System.out.println(ssCpuSystem);

            // Percentages of idle CPU time (ssCpuIdle)
            String ssCpuIdle = client.getAsString(new OID(".1.3.6.1.2.1.2.2.1.16"));
            System.out.println(ssCpuIdle);

            // 1 minute Load (laLoad.1)
            String laLoad1 = client.getAsString(new OID(".1.3.6.1.2.1.2.2.1.20"));
            System.out.println(laLoad1);

            // 5 minute Load (laLoad.2)
            String laLoad2 = client.getAsString(new OID(".1.3.6.1.4.1.2021.10.1.3.2"));
            System.out.println(laLoad2);

            // 15 minute Load (laLoad.3)
            String laLoad3 = client.getAsString(new OID(".1.3.6.1.4.1.2021.10.1.3.3"));
            System.out.println(laLoad3);

            // Total Swap Size configured for the host (memTotalSwap)
            String memTotalSwap = client.getAsString(new OID(".1.3.6.1.4.1.2021.4.3"));
            System.out.println(memTotalSwap);

            // Available Swap Space on the host (memAvailSwap)
            String memAvailSwap = client.getAsString(new OID(".1.3.6.1.4.1.2021.4.4"));
            System.out.println(memAvailSwap);

            // Total Real/Physical Memory Size on the host (memTotalReal)
            String memTotalReal = client.getAsString(new OID(".1.3.6.1.4.1.2021.4.5"));
            System.out.println(memTotalReal);

            // Available Real/Physical Memory Space on the host (memAvailReal)
            String memAvailReal = client.getAsString(new OID(".1.3.6.1.4.1.2021.4.6"));
            System.out.println(memAvailReal);

            // Total RAM Free (memTotalFree)
            String memTotalFree = client.getAsString(new OID(".1.3.6.1.4.1.2021.4.11.0"));
            System.out.println(memTotalFree);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
