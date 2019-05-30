package Bsnmp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Monitor extends Thread
{
    final static Logger logger = LogManager.getLogger(Monitor.class);

    final String IP_MAIL = "localhost";
    final int PORT_MAIL = 2020;

    private Address address = GenericAddress.parse("udp:127.0.0.1/161");
    String[] ADMON_MAIL;
    private String nameDevice = "";
    int maxMemOnDevice = 8;
    int maxDiskSpace = 1000;

    private int onCheckObjects = 3; // maximos objetos escaneados
    private int alertMem = 80; // DEFAULT ALERT GRADE ON %
    private int alertDisk = 80; // DEFAULT ALERT GRADE ON %
    private int timeForNotify = 60000; // tiempo de espera antes de enviar una alerta nuevamente
    private int timeFor = 0; // a que dispositivo se le aplicara el tiempo de espera
    private String connInfo = "";
    private String memInfo = "";
    private String diskInfo = "";

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
        BAD_TIMER_EXECUTION,
        MAIL_CONNECTION,

    }

    private errorCode eCode = errorCode.EXITO;

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
        address = "udp:" + address + "/161";
        this.address = GenericAddress.parse(address);
        logger.info("Generando conexion SNMP hacia " + this.address);
        try
        {
            tm = new DefaultUdpTransportMapping();
        }
        catch (SocketException ex)
        {
            ex.printStackTrace();
            eCode = errorCode.UDP_ERROR_INSTANCE;
            logger.error("Error - Codigo " + eCode + "Falla en protocolo UDP ");
        }
        sn = new Snmp(tm);
    }

    /***
     * Inicia el monitoreo de la instancia seleccionada proporcionando el porcentaje
     * de alerta para establecer estado critico en los componentes del sistema
     * monitoreado.
     * 
     * @param alertCpu
     * @param alertDisk
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public Monitor(String nameDevice, String address, int alertMem, int alertDisk)
            throws SAXException, ParserConfigurationException, IOException
    {
        this.nameDevice = nameDevice;
        this.alertMem = alertMem;
        this.alertDisk = alertDisk;
        address = "udp:" + address + "/161";
        this.address = GenericAddress.parse(address);
        logger.info("Generando conexion SNMP hacia " + this.address);
        this.address = GenericAddress.parse(address);
        init();

    }

    public void init()
            throws IOException, SAXException, ParserConfigurationException
    {
        tm = new DefaultUdpTransportMapping();
        sn = new Snmp(tm);
        for (int i = 0; i < timer.length; i++)
        {
            timer[i] = new TimerComponentes(timeForNotify);
        }
        switch (nameDevice)
        {
        // en KB
        case "Base de datos":
            maxMemOnDevice = 8000000 * (alertMem / 100); // 8gb de ram
            maxDiskSpace = 1000000000 * (alertDisk / 100); // 1 tera
            break;
        case "Boletazo server":
            maxMemOnDevice = 8000000 * (alertMem / 100); // 8gb de ram
            maxDiskSpace = 1000000000 * (alertDisk / 100); // 1 tera
            break;
        }
        alertMem = maxMemOnDevice * (alertMem / 100);
        alertDisk = maxDiskSpace * (alertDisk / 100); // 1 tera
        initMailDirectory();
    }

    private void initMailDirectory()
            throws SAXException, IOException, ParserConfigurationException
    {
        File xmlDirectory = new File("C:\\opt\\Git\\Bsnmp\\src\\main\\java\\Bsnmp\\DistributionMail.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlDirectory);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("staff");
        ADMON_MAIL = new String[nList.getLength()];
        for (int temp = 0; temp < nList.getLength(); temp++)
        {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE)
            {
                Element eElement = (Element) nNode;
                ADMON_MAIL[temp] = eElement.getElementsByTagName("email").item(0).getTextContent();
                // logger.debug("Email: " + ADMON_MAIL[temp]);
            }
        }
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
            eCode = errorCode.UDP_ERROR_INSTANCE;
            logger.error("Error - con codigo " + eCode + " agente no iniciado en " + nameDevice);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            eCode = errorCode.BAD_TIMER_EXECUTION;
            logger.error("Error - Interrupcion pronta en HILO de timer");
        }
        catch (IOException ex)
        {
            eCode = errorCode.BAD_DATA_READER;
            ex.printStackTrace();
            logger.error("Error IO - Con codigo " + eCode + " GENERIC ERROR");
        }
    }

    /***
     * Inicia el monitoreo sobre los componentes del sistema.
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws NullPointerException
     */
    private void secuencialMonitoring()
            throws IOException, InterruptedException, RuntimeException
    {
        boolean alive = true;
        while (alive)
        {

            memInfo = getAsString(new OID(".1.3.6.1.4.1.2021.4.11.0"));
            // logger.debug("Monitoreo MemoryFree Kb ::: " + memInfo);
            diskInfo = getAsString(new OID(".1.3.6.1.4.1.2021.9.1.7.1"));
            // logger.debug("Monitoreo SpaceFreeOnDisk ::: " + diskInfo);
            connInfo = getAsString(
                    new OID(".1.3.6.1.4.1.8072.1.3.2.3.1.1.12.115.110.109.112.100.95.115.116.97.116.117.115"));
            // logger.debug("Monitoreo ConnectionDB ::: " + connInfo);
            analyzeData(memInfo, diskInfo, connInfo);
            Thread.sleep(2000);
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
    private void analyzeData(String dataMemory, String dataDisk, String connDB)
    {
        int[] analyzerArray = new int[3]; // Storage for data analyzer
        try
        {

            analyzerArray[0] = Integer.parseInt(dataMemory);
            analyzerArray[1] = Integer.parseInt(dataDisk);
            if (connDB.contains("successfull"))
            {
                analyzerArray[2] = 0;
            }
            else
            {
                analyzerArray[2] = -1;
            }

        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            eCode = errorCode.BAD_DATA_INCOMING;
            logger.error("Error - BadData Format data incoming on snmp");
        }

        for (int i = 0; i < onCheckObjects; i++)
        {
            boolean notificaAdmon = false;
            switch (i)
            {
            case 0:
                if (analyzerArray[0] < (alertMem) && !timer[0].isAlive())
                {
                    logger.info("ALERTA - LA MEMORIA RAM DEL DISPOSITIVO ESTA BAJA.");
                    timeFor = timeClass.MEMORY.ordinal();
                    timer[timeFor].start();
                    notificaAdmon = true;
                }
                break;
            case 1:
                if (analyzerArray[1] < (alertDisk) && !timer[1].isAlive())
                {
                    logger.info("ALERTA - EL DISCO ESTA CASI LLENO, ASEGURESE DE LIBERAR ESPACIO.");
                    timeFor = timeClass.DISK.ordinal();
                    timer[timeFor].start();
                    notificaAdmon = true;
                }
                break;
            case 2:
                if (analyzerArray[2] == -1 && !timer[2].isAlive() && nameDevice.equalsIgnoreCase("Base de datos")) // -1
                {
                    logger.info("Conexion perdida con la base de datos, favor de reestablecerla. " + nameDevice);
                    timeFor = timeClass.CONNECTION.ordinal();
                    timer[timeFor].start();
                    notificaAdmon = true;
                }
                break;
            }
            // manda correo a todos los administradores
            if (notificaAdmon)
            {
                for (int temp = 0; temp < ADMON_MAIL.length; temp++)
                {
                    alertaEmail(timeFor, ADMON_MAIL[temp]);
                }
            }
        }
    }

    /***
     * Encargado de realizar las solicitudes hacia el servidor Mail, con el asunto y
     * texto definidos de manera predeterminada.
     * 
     * @param msgCode
     */
    private void alertaEmail(int msgCode, String destinatario)
    {
        logger.info("Enviando ALERTA AL ADMINISTRADOR.");
        Socket clientSocket = null;
        try
        {
            clientSocket = new Socket(IP_MAIL, PORT_MAIL);
            OutputStream outStream = clientSocket.getOutputStream();
            DataOutputStream flowOut = new DataOutputStream(outStream);
            switch (msgCode)
            {
            case 0:
                flowOut.writeUTF(destinatario + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.MEMORY
                        + " se detecto con un uso de %" + memInfo + " favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            case 1:
                flowOut.writeUTF(destinatario + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.DISK
                        + " se detecto con un uso de %" + diskInfo + " favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            case 2:
                flowOut.writeUTF(destinatario + ",ALERTA EN " + nameDevice + " URGENTE,El dispositivo "
                        + nameDevice + " acaba de presentar una falla en el sistema del tipo " + timeClass.CONNECTION
                        + " favor de tomar accion inmediatamente,1");
                clientSocket.close();
                break;
            default:
            }
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            eCode = errorCode.MAIL_CONNECTION;
            logger.error(
                    "Ocurrio un error al intentar conectarse al host: [" + IP_MAIL + "] y puerto: [" + PORT_MAIL + "]");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            eCode = errorCode.MAIL_CONNECTION;
            logger.error(
                    "Ocurrio un error al establecer el canal de datos: [" + IP_MAIL + "] y puerto [" + PORT_MAIL + "]");
        }
    }

    /**
     * Realiza la instancia de busqueda sobre el OID especificado y lo convierte a
     * un String.
     * 
     * @param oid
     * @return
     * @throws IOException
     */
    private String getAsString(OID oid) throws IOException
    {
        ResponseEvent event = get(new OID[] {oid});
        return event.getResponse().get(0).getVariable().toString();
    }

    /**
     * Peticion al agente para devolver el evento OID especificado en caso
     * contrario, generara un error sobre la conexion o fallo de busqueda
     * 
     * @param oids
     * @return
     * @throws IOException
     */
    private ResponseEvent get(OID oids[]) throws IOException
    {
        PDU pdu = new PDU();
        for (OID oid : oids)
        {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        ResponseEvent event = sn.send(pdu, getTarget(), null);
        if (event != null) { return event; }
        throw new RuntimeException("GET timed out");
    }

    /**
     * Establece el enlace sobre la direccion del agente y entra en la comunidad
     * definida tambien por el mismo agente.
     * 
     * @return Target
     */
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
