package no.entra.bacnet.agent.commands.cov;

import no.entra.bacnet.json.bvlc.BvlcFunction;
import no.entra.bacnet.objects.ObjectId;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static no.entra.bacnet.internal.objects.ObjectIdMapper.toHexString;
import static no.entra.bacnet.json.apdu.SDContextTag.*;
import static no.entra.bacnet.json.objects.PduType.ConfirmedRequest;
import static no.entra.bacnet.json.services.ConfirmedServiceChoice.SubscribeCov;
import static no.entra.bacnet.json.utils.HexUtils.octetFromInt;
import static org.slf4j.LoggerFactory.getLogger;

/*
Subscribe to a single parameter for Change of Value(COV)
 */
public class UnConfirmedSubscribeCovCommand extends SubscribeCovCommand {
    private static final Logger log = getLogger(UnConfirmedSubscribeCovCommand.class);
    public static final String UNCONFIRMED = "00";

    public UnConfirmedSubscribeCovCommand(InetAddress sendToAddress, int subscriptionId, ObjectId subscribeToSensorId) throws IOException {
        super(sendToAddress, subscriptionId, subscribeToSensorId);
    }

    public UnConfirmedSubscribeCovCommand(DatagramSocket socket, InetAddress sendToAddress, int subscriptionId, ObjectId subscribeToSensorId) throws IOException {
        super(socket, sendToAddress, subscriptionId, subscribeToSensorId);
    }

    @Override
    protected String buildHexString() {
        ObjectId deviceSensorId = getSubscribeToSensorIds().get(0);
        return buildUnConfirmedCovSingleRequest(deviceSensorId);
    }

    /**
     * Create HexString for a Confirmed COV Request to local net, and a single sensor.
     * @return hexString with bvlc, npdu and apdu
     * @param deviceSensorId
     */
    protected String buildUnConfirmedCovSingleRequest(ObjectId deviceSensorId) {
        String hexString = null;
        String objectIdHex = toHexString(deviceSensorId);
        String confirmEveryNotification = UNCONFIRMED;
        String lifetimeHex = octetFromInt(0).toString(); //indefinite

        String pduTypeHex = ConfirmedRequest.getPduTypeChar() + "0";
        String serviceChoiceHex = SubscribeCov.getServiceChoiceHex();
        String invokeIdHex = getInvokeId().toString();
        String maxApduLengthHex = "02"; //TODO need to be able to set this.;
        //When a client have multiple processes subscribing to the server. Use this parameter to route notifications to the
        //corresponding client process. - Not much in use in a Java implementation.
        String subscriberProcessIdentifier = getSubscriptionIdHex().toString();
        String apdu = pduTypeHex + maxApduLengthHex + invokeIdHex + serviceChoiceHex + TAG0LENGTH1 +
                subscriberProcessIdentifier + TAG1LENGTH4 + objectIdHex + TAG2LENGTH1 +
                confirmEveryNotification + TAG3LENGTH1 + lifetimeHex;
        /*
        00 = PDUType = 0
        02 = Max APDU size = 206
        0f = invoke id = 15 // Identify multiple messages/segments for the same request.
        05 = Service Choice 5 - SubscribeCOV-Request
        09 = SD Context Tag 0, Subscriber Process Identifier, Length = 1
        12 = 18 integer
        1c = SD context Tag 1, Monitored Object Identifier, Length = 4
        00000000 = Analog Input, Instance 0
        29 = SD context Tag 2, Issue Confirmed Notification, Length = 1
        01 = True, 00 = false
        39 = SD context Tag 3, Lifetime, Length = 1
        00 = 0 integer == indefinite
         */
        String npdu = "0120ffff00ff"; //TODO need to objectify this.
        int numberOfOctets = (apdu.length() + npdu.length() + 8) / 2;
        String messageLength = Integer.toHexString(numberOfOctets);
        if (numberOfOctets <= 255) {
            messageLength = "00" + messageLength;
        }
        String bvlc = "81" + BvlcFunction.OriginalUnicastNpdu.getBvlcFunctionHex() + messageLength;

        hexString = bvlc + npdu + apdu;
        log.debug("Hex to send: {}", hexString);
        return hexString;
    }
}
