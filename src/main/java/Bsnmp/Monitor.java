package Bsnmp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
    final int PORT_MAIL = 2020;

    private String nameDevice = "";
    private Address address = GenericAddress.parse("udp:127.0.0.1/161");
    private int onCheckObjects = 3; // maximos objetos escaneados
    private int alertCpu = 80; // DEFAULT ALERT GRADE ON %
    private int alertDisk = 80; // DEFAULT ALERT GRADE ON %
    private int timeForNotify = 60000; // tiempo de espera antes de enviar una alerta nuevamente
    private int timeFor = 0; // a que dispositivo se le aplicara el tiempo de espera
    String connInfo = "";
    String memInfo = "";
    String diskInfo = "";

    private TransportMapping tm; // = new DefaultUdpTransportMapping();
    private Snmp sn;
    private Thread[] timer = new TimerComponentes[3]; // EN ORDE

    private enum timeClass {
        MEMORY,
        DISK,
        CONNECTION;
    }

    private enum estado {
        ESTABLE,
        ALERTA,
        CRITICO,
        ERROR,
    }

    private estado state = estado.ESTABLE;

    private enum errorCode {
        EXITO,
        UDP_ERROR_INSTANCE,
        BAD_DATA_READER,
        BAD_DATA_INCOMING,
    }

    private errorCode eCode = errorCode.EXITO;

    /***
     * Inicia el monitoreo de la instancia seleccionada proporcionando el porcentaje
     * de alerta para establecer estado critico en los componentes del sistema
     * monitoreado.
     * 
     * @param alertCpu
     * @param alertDisk
     * @throws IOException
     */
    public Monitor(String nameDevice, String address, int alertCpu, int alertDisk)
    {
        this.nameDevice = nameDevice;
        this.alertCpu = alertCpu;
        this.alertDisk = alertDisk;
        this.address = GenericAddress.parse("udp:" + address + "/161");
        logger.info("Generando conexion SNMP hacia " + this.address);
        this.address = GenericAddress.parse(address);
        try
        {
            init();
        }
        catch (SocketException ex)
        {
            ex.printStackTrace();
            logger.error("Error - Falla en protocolo UDP ");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.error("Error - Falla en protocolo UDP ");
        }
    }

    public void init() throws IOException
    {
        tm = new DefaultUdpTransportMapping();
        sn = new Snmp(tm);
        for (int i = 0; i < timer.length; i++)
        {
            timer[i] = new TimerComponentes(timeForNotify);
        }
    }

    /***
     * Inicia el monitoreo de la instancia seleccionada con un nivel de alerta de
     * 80% para iniciar estado critico en los componentes del sistema monitoreado
     * 
     * @param address
     * @throws IOException
     */
    public Monitor(String nameDevice, String address) throws IOException
    {
        this.nameDevice = nameDevice;
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
        catch (RuntimeException e)
        {
            e.printStackTrace();
            logger.error("Error - Agente no iniciado");
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
    private void secuencialMonitoring() throws IOException, InterruptedException, RuntimeException
    {
        boolean alive = true;
        while (alive)
        {

            memInfo = getAsString(new OID("1.3.6.1.4.1.2021.4.11.0"));
            logger.debug("Monitoreo MemoryFree Kb :::  " + memInfo);
            diskInfo = getAsString(new OID(".1.3.6.1.4.1.2021.9.1.7.1"));
            logger.debug("Monitoreo SpaceFreeOnDisk :::  " + diskInfo);
            connInfo = getAsString(new OID(".1.3.6.1.4.1.2021.4.11.0"));
            logger.debug("Monitoreo ConnectionDB :::  " + connInfo);

            Thread.sleep(5000);
            analyzeData(memInfo, diskInfo, connInfo);
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
    private void analyzeData(String dataMemory, String diskMemory, String connDB)
    {
        int[] analyzerArray = new int[3]; // Storage for data analyzer
        try
        {

            analyzerArray[0] = Integer.parseInt("100");
            analyzerArray[1] = Integer.parseInt("100");
            analyzerArray[2] = Integer.parseInt("100");

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
                if (analyzerArray[0] < (alertCpu - 100) && !timer[0].isAlive())
                {
                    logger.info("ALERTA - LA MEMORIA RAM DEL DISPOSITIVO ESTA BAJA.");
                    timeFor = timeClass.MEMORY.ordinal();
                    timer[timeFor].start();
                }
                break;
            case 1:
                if (analyzerArray[1] < (alertDisk - 100) && !timer[1].isAlive())
                {
                    logger.info("ALERTA - EL DISCO ESTA CASI LLENO, ASEGURESE DE LIBERAR ESPACIO.");
                    timeFor = timeClass.DISK.ordinal();
                    timer[timeFor].start();
                }
                break;
            case 2:
                if (analyzerArray[2] == -1 && !timer[2].isAlive()) // -1 INDICA, DESCONEXION
                {
                    logger.info("Conexion perdida con la base de datos, favor de reestablecerla.");
                    timeFor = timeClass.CONNECTION.ordinal();
                    timer[timeFor].start();
                }
                break;
            default:
                break;
            }
        }
    }

    private void alertaEmail(int msgCode)
    {
        logger.info("Enviando solicitud del cliente.");
        Socket clientSocket = null;
        try
        {
            clientSocket = new Socket(IP_MAIL, PORT_MAIL);
            OutputStream outStream = clientSocket.getOutputStream();
            DataOutputStream flowOut = new DataOutputStream(outStream);
            switch (msgCode)
            {
            case 0:
                flowOut.writeUTF(ADMON_MAIL + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.MEMORY
                        + ", se detecto con un uso de %" + memInfo + ", favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            case 1:
                flowOut.writeUTF(ADMON_MAIL + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.DISK
                        + ", se detecto con un uso de %" + diskInfo + ", favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            case 2:
                flowOut.writeUTF(ADMON_MAIL + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.CONNECTION
                        + ", se detecto con un uso de %" + connInfo + ", favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            default:
            }
        }
        catch (UnknownHostException e)
        {
            logger.error(
                    "Ocurrio un error al intentar conectarse al host: [" + IP_MAIL + "] y puerto: [" + PORT_MAIL + "]");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            logger.error(
                    "Ocurrio un error al establecer el canal de datos: [" + IP_MAIL + "] y puerto [" + PORT_MAIL + "]");
            e.printStackTrace();
        }
    }

    private String getAsString(OID oid) throws IOException
    {
        ResponseEvent event = get(new OID[] {oid});
        return event.getResponse().get(0).getVariable().toString();
    }

    private ResponseEvent get(OID oids[]) throws IOException
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
