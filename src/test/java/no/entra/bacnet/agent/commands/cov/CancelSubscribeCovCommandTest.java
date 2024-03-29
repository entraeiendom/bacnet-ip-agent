package no.entra.bacnet.agent.commands.cov;

import no.entra.bacnet.objects.ObjectId;
import no.entra.bacnet.objects.ObjectType;
import no.entra.bacnet.octet.Octet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CancelSubscribeCovCommandTest {

    private CancelSubscribeCovCommand covCommand;
    private DatagramSocket socket;
    private int suscriptionId;

    @Before
    public void setUp() throws Exception {
        socket = mock(DatagramSocket.class);
        ObjectId analogValue1 = new ObjectId(ObjectType.AnalogValue, 1);
        InetAddress sendToAddress = SubscribeCovCommand.inetAddressFromString("10.10.10.10");
        suscriptionId = 18;
        covCommand = new CancelSubscribeCovCommand(socket, sendToAddress, suscriptionId, analogValue1 );
        covCommand.setInvokeId(new Octet("01"));

    }

    @Test
    public void executeTest() throws IOException {
        covCommand.execute();
        verify(socket,  times(1)).send(any());
    }

    @Test
    public void buildCancelSubscribeCovHexString() {
        String expected = "810a00150120ffff00ff0002010509121c00800001";
        String hexString = covCommand.buildHexString();
        assertEquals(expected, hexString);
    }
}