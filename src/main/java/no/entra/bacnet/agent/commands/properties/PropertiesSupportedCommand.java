package no.entra.bacnet.agent.commands.properties;

import no.entra.bacnet.json.bvlc.BvlcFunction;
import no.entra.bacnet.json.objects.PropertyIdentifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;

import static no.entra.bacnet.agent.utils.ByteHexConverter.hexStringToByteArray;
import static no.entra.bacnet.json.apdu.SDContextTag.TAG1LENGTH1;
import static no.entra.bacnet.json.apdu.SDContextTag.TAG2LENGTH4;
import static no.entra.bacnet.json.utils.HexUtils.intToHexString;
import static org.slf4j.LoggerFactory.getLogger;

/*
Also named "object-list service" in Bacnet. aka which input and output this device do see.
 */
public class PropertiesSupportedCommand {
    private static final Logger log = getLogger(PropertiesSupportedCommand.class);

    private DatagramSocket socket;
    public static final int BACNET_DEFAULT_PORT = 47808;
    private byte[] buf = new byte[2048];
    private InetAddress sendToAddress;

    public PropertiesSupportedCommand() throws SocketException {
        socket = new DatagramSocket(null);
        socket.setBroadcast(true);
        socket.setReuseAddress(true);
    }

    void broadcast() throws IOException {
        local("255.255.255.255");
    }

    void local(String ipv4Address) throws IOException {
        SocketAddress inetAddress = new InetSocketAddress(BACNET_DEFAULT_PORT);
        sendToAddress = InetAddress.getByName(ipv4Address);
        socket.bind(inetAddress);
        sendWhoHas();
    }

    private void sendWhoHas() throws IOException {

        /*
        BVLC 810b000c
        NPUDU 0120ffff00ff
        X'10' PDU Type=1 (Unconfirmed-Service-Request-PDU)
        X'07' Service Choice=7 (Who-Has-Request)
        X'2C' SD Context Tag 2 (Object Identifier, L=4)
        X'00000001' Analog Input, Instance Number=1
         */
        String apdu = "1007" + TAG2LENGTH4 + "00000000";
        String npdu = "0120ffff00ff";
        int numberOfOctets = (apdu.length() + npdu.length() + 8) / 2;
        String messageLength = Integer.toHexString(numberOfOctets);
        if (numberOfOctets <= 255) {
            messageLength = "00" + messageLength;
        }
        String bvlc = "81" + BvlcFunction.OriginalBroadcastNpdu.getBvlcFunctionHex() + messageLength;

        String servicesSupported = PropertyIdentifier.ProtocolServicesSupported.getPropertyIdentifierHex();
        String hexDeviceNumber = intToHexString(1001, 6);
        apdu = "0275000c0c02" + hexDeviceNumber + TAG1LENGTH1 + servicesSupported;
        npdu = "0104";
        bvlc = "810a0011";
        String hexString = bvlc + npdu + apdu;
        log.debug("Hex to send: {}", hexString);

        buf = hexStringToByteArray(hexString);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, sendToAddress, BACNET_DEFAULT_PORT);
        log.debug("Sending: {}", packet);
        socket.send(packet);
    }

    private void disconnect() {
        if (socket != null && socket.isConnected()) {
            socket.disconnect();
            socket = null;
        }
    }

    public static void main(String[] args) {
        PropertiesSupportedCommand client = null;

        //Destination may also be fetched as the first program argument.
        String destination = null;
        if (args.length > 0) {
            destination = args[0];
        }
        try {
            client = new PropertiesSupportedCommand();
            if (destination == null) {
                client.broadcast();
            } else {
                client.local(destination);
            }
            Thread.sleep(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.disconnect();
        }
    }
}
