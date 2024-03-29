package no.entra.bacnet.agent.commands.cov;

import no.entra.bacnet.objects.ObjectId;
import no.entra.bacnet.objects.ObjectType;
import no.entra.bacnet.octet.Octet;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static no.entra.bacnet.utils.HexUtils.octetFromInt;
import static org.junit.Assert.*;

public class SubscribeCovCommandBuilderTest {

    private ObjectId analogInput0;
    private InetAddress sendToAddress;
    private int subscriptionId;

    @Before
    public void setUp() throws Exception {
        subscriptionId = 18;
        analogInput0 = new ObjectId(ObjectType.AnalogInput, 0);
        sendToAddress = SubscribeCovCommand.inetAddressFromString("10.10.10.10");
    }

    @Test
    public void buildCOVSingleSensorUnConfirmedTest()  {
        String expected = "810a00190120ffff00ff00020f0509121c0000000029003900";
        SubscribeCovCommand covCommand = new SubscribeCovCommandBuilder(sendToAddress, analogInput0)
                .withSubscriptionId(subscriptionId)
                .withInvokeId(new Octet("0f"))
                .withConfirmedNotifications(false)
                .build();
        assertTrue(covCommand instanceof UnConfirmedSubscribeCovCommand);
        String hexString = covCommand.buildHexString();
        assertEquals(expected, hexString);
        assertNotNull(covCommand.getSendToAddress());
    }

    @Test
    public void buildCOVSingleSensorConfirmedTest()  {
        int lifetimeSeconds = 50;
        Octet lifetimeHex = octetFromInt(lifetimeSeconds);
        assertEquals("32", lifetimeHex.toString());
        String expected = "810a00190120ffff00ff00020f0509121c00000000290139" + lifetimeHex;
        SubscribeCovCommand covCommand = new SubscribeCovCommandBuilder(sendToAddress, analogInput0)
                .withSubscriptionId(subscriptionId)
                .withInvokeId(new Octet("0f"))
                .withConfirmedNotifications(true)
                .withLifetime(lifetimeSeconds)
                .build();
        assertTrue(covCommand instanceof ConfirmedSubscribeCovCommand);

        String hexString = covCommand.buildHexString();
        assertEquals(expected, hexString);
    }

    @Test(expected = IllegalStateException.class)
    public void verifyLifetimeAndUnconfirmedIsIllegal()  {
        SubscribeCovCommand covCommand = new SubscribeCovCommandBuilder(sendToAddress, analogInput0)
                .withSubscriptionId(subscriptionId)
                .withInvokeId(new Octet("0f"))
                .withLifetime(5)
                .withConfirmedNotifications(false)
                .build();
        fail("Should not be reached");
    }
}