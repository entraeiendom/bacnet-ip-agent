package no.entra.bacnet.agent.mqtt;

import no.entra.bacnet.agent.devices.DeviceId;
import no.entra.bacnet.rec.RealEstateCore;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetAddress;
import java.security.*;
import java.util.Optional;

import static no.entra.bacnet.agent.utils.PropertyReader.findProperty;
import static org.slf4j.LoggerFactory.getLogger;

public class PahoMqttClient implements no.entra.bacnet.agent.mqtt.MqttClient {

    private static final Logger log = getLogger(PahoMqttClient.class);

    public static final String MQTT_BROKER_URL = "MQTT_BROKER_URL";
    public static final String MQTT_USERNAME = "MQTT_USERNAME";
    public static final String MQTT_PASSWORD = "MQTT_PASSWORD";
    public static final String MQTT_TOPIC = "MQTT_TOPIC";
    public static final String MQTT_CLIENT_ID = "MQTT_CLIENT_ID";
    public static final int AT_MOST_ONCE = 0; //Best Effort
    public static final int AT_LEAST_ONCE = 1; //Require ack from client to server on every message.
    public static final int EXACTLY_ONCE = 2; //Require ack from client to server on every message.
    private final String brokerUrl;
    private String username = null;
    private String password = null;
    private String topic = null;
    private String clientId = null;
    private final boolean cleanSession;
    private MqttClient mqttClient;
    private final MemoryPersistence persistence;
    private boolean isConnected = false;
    private int qos = AT_MOST_ONCE;

    public PahoMqttClient(String brokerUrl) {
        this.brokerUrl = brokerUrl;
        this.persistence = new MemoryPersistence();
        this.cleanSession = true;
        connect();
    }

    /*
    Used for testing
     */
    protected PahoMqttClient() {
        this.cleanSession = true;
        this.brokerUrl = null;
        this.persistence = new MemoryPersistence();
    }

    public PahoMqttClient(String brokerUrl, String username, String password, String topic, String clientId) {
        this.cleanSession = true;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.clientId = clientId;
        this.persistence = new MemoryPersistence();
        connect();
    }

    public void connect()  {
        try {
            if (mqttClient != null) {
                log.info("MqttClient is instanciated. Will check for connectivity");
                if (!mqttClient.isConnected()) {
                    log.info("MqttClient is not connected. Will try to reconnect");
                    mqttClient.reconnect();
                }
            } else {
                log.info("Start new connection.");
                MqttConnectOptions connOpts = new MqttConnectOptions();
                if (brokerUrl.startsWith("ssl") || brokerUrl.startsWith("mqtts")) {
                    try {
                        SSLContext sslContext = SSLContext.getInstance("SSL");
                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        KeyStore keyStore = KeyStore.getInstance("jks"); //readKeyStore();
                        trustManagerFactory.init(keyStore);
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
                        sslContext.init(null, new BlindTrustManager[]{new BlindTrustManager()}, new SecureRandom());
                        connOpts.setSocketFactory(sslContext.getSocketFactory());
                    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                        log.info("Failed to setup SSL connection to broker: {}. REason: {}", brokerUrl, e.getMessage());
                    }
                }


                log.info("Connect to {} with clientId: {}", brokerUrl, clientId);
                mqttClient = new MqttClient(brokerUrl, clientId, persistence);

                if (username != null) {
                    connOpts.setUserName(username);
                }
                if (password != null) {
                    connOpts.setPassword(password.toCharArray());
                }
                //Ensure subscriptions are stil alive after re-connect. Set to false.
                connOpts.setCleanSession(cleanSession);
                //Ping when no activity for 10 seconds
                connOpts.setKeepAliveInterval(10);
                //Reconnect when broker or network terminates
                connOpts.setAutomaticReconnect(true);
                log.trace("Connecting to broker: {} ", brokerUrl);
                mqttClient.connect(connOpts);
                log.info("Connected to broker: {}", brokerUrl);
                this.isConnected = true;

            }
        } catch (MqttException me) {
            log.info("Mqtt failed to connect. BrokerUrl: {}, reason: {}", brokerUrl, me.getMessage());
            me.printStackTrace();
        }
    }

    @Override
    public void publishRealEstateCore(RealEstateCore recMessage, DeviceId recDeviceId, Optional<InetAddress> sourceAddress) {
        String recJson = recMessage.toJson();

        MqttMessage message = new MqttMessage(recJson.getBytes());
        String fullTopic = topic + "/rec";
        log.trace("Publish to MQTT. Topic: {} Message: {} ", fullTopic, recJson);
        if (recDeviceId != null && recDeviceId.getId() != null) {
            fullTopic += "/" + recDeviceId.getId();
        }
        sendMessage(message, fullTopic);
    }

    @Override
    public void publishUnknownHexString(String hexString) {
        String fullTopic = topic + "/unknownHex";
        String unknownJson = "{ \"hexString\": \"" + hexString + "\"}";
        log.trace("Publish to MQTT. FullTopic: {} Message: {}", fullTopic, unknownJson);
        MqttMessage message = new MqttMessage(unknownJson.getBytes());
        sendMessage(message, fullTopic);
    }

    void sendMessage(MqttMessage message, String fullTopic) {
        message.setQos(qos);
        try {
            if (isConnected) {
                mqttClient.publish(fullTopic, message);
            } else {
                log.warn("No connection to mqtt broker on url: {}", brokerUrl);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (Exception e) {
                log.trace("Failed to disconnect due to: {}", e.getMessage());
            }
        }
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getTopic() {
        return topic;
    }

    public String getClientId() {
        return clientId;
    }

    public MqttClient getMqttClient() {
        return mqttClient;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public static void main(String[] args) {
        String username = findProperty(MQTT_USERNAME);
        String password = findProperty(MQTT_PASSWORD);
        String brokerUrl = findProperty(MQTT_BROKER_URL);
        String topic = findProperty(MQTT_TOPIC);
        String clientId = findProperty(MQTT_CLIENT_ID) + "test";
        if (username == null || username.isEmpty() || password == null || password.isEmpty() ) {
            System.out.println("Please run with java Main -DMQTT_USERNAME=... -DmqttPassword=... -DmqttBrokerUrl=... -DmqttClientId=... -DmqttTopic=...");
            System.exit(1);
        }

        PahoMqttClient pahoMqttClient = new PahoMqttClient(brokerUrl, username, password, topic, clientId);
        System.out.println("Connected");
        String content = "Test to Mqtt";
        System.out.println("Publishing message: " + content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(AT_MOST_ONCE);
        pahoMqttClient.sendMessage(message, topic);
        System.out.println("Message published");
        pahoMqttClient.disconnect();
        System.out.println("Disconnected");
        System.exit(0);
    }

    public static void mainold(String[] args) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        String topic = "bacnet-ip-agent";
        String content = "Message from MqttPublishSample";
        int qos = 2;
        String broker = "tcp://mqtt.eclipse.org:1883";
        String clientId = "bacnet-ip-agent";
        MemoryPersistence persistence = new MemoryPersistence();

        if (System.getProperty("MQTT_BROKER") != null) {
            broker = System.getProperty("MQTT_BROKER");
        }
        MqttConnectOptions connOpts = new MqttConnectOptions();
        if (broker.startsWith("ssl") || broker.startsWith("mqtts")) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("jks"); //readKeyStore();
            trustManagerFactory.init(keyStore);
//            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            sslContext.init(null, new BlindTrustManager[]{new BlindTrustManager()}, new SecureRandom());
            connOpts.setSocketFactory(sslContext.getSocketFactory());
        }

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            connOpts.setCleanSession(true);
            connOpts.setUserName("user");
            connOpts.setPassword("password".toCharArray());
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: " + content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }
}
