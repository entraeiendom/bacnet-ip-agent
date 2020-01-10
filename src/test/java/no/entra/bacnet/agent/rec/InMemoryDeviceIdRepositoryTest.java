package no.entra.bacnet.agent.rec;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryDeviceIdRepositoryTest {

    private DeviceIdRepository idRepository;
    private DeviceId defaultDeviceId;
    public static final String DEFAULT_PORT = "47808";

    @Before
    public void setUp() throws Exception {
        idRepository = new InMemoryDeviceIdRepository();
        defaultDeviceId = new DeviceId("id1", "127.0.0.1", DEFAULT_PORT, "TFM123/456", 2002 );
        //String id, String ipAddress, String portNumber, String tfmTag, int instanceNumber
        idRepository.add(defaultDeviceId);
        idRepository.add(new DeviceId("id3", "127.0.0.1", DEFAULT_PORT, "TFM123/6767", 2002 ));
        idRepository.add(new DeviceId("id4", "127.0.0.2", DEFAULT_PORT, "TFM123/7878", 2004 ));
    }

    @Test
    public void add() {
        assertEquals(3, idRepository.size());
        idRepository.add(new DeviceId("id2", "127.0.0.1", DEFAULT_PORT, "TFM123/456", 2002));
        assertEquals(4, idRepository.size());
    }

    @Test(expected = IllegalStateException.class)
    public void addConflict() {
        idRepository.add(new DeviceId("id1"));
    }

    @Test
    public void findFromTfm() {
        assertEquals(1, idRepository.findFromTfm("TFM123/456").size());
    }

    @Test
    public void findFromInstanceNumber() {
        assertEquals(2, idRepository.findFromInstanceNumber(2002).size());
    }

    @Test
    public void findFromIpAddress() {
        assertEquals(2, idRepository.findFromIpAddress("127.0.0.1").size());
    }
}