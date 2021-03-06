# backnet-ip-agent
![BuildStatus](https://travis-ci.com/entraeiendom/bacnet-ip-agent.svg?branch=master)

Listen to UDP packages, and send UDP packages.

## Getting Started

1. Create an IoT-Edge in Azure-IoT.
2. Copy the Primary Connection String
3. Run!
*Run in Linux/Windows/OS-X*
```
docker run -p 0.0.0.0:47808:47808 -p 0.0.0.0:47808:47808/udp \
  --name=bacnet-ip-agent-java -e DEVICE_CONNECTION_STRING="string from 2. above" baardl/bacnet-ip-agent-java:alpine-0.3-STABLE
```
*Run in RaspberryPi/ARM*
```
docker run -p 0.0.0.0:47808:47808 -p 0.0.0.0:47808:47808/udp \
  --name=bacnet-ip-agent-java -e DEVICE_CONNECTION_STRING="string from 2. above" baardl/bacnet-ip-agent-java:armv7-0.3.2-STABLE
```
4. Verify
```
az iot hub monitor-events --hub-name <your hub name from 1. above>
```


## Development

A good starting point is to understand this code: [BlockingRecordAndForwardObserver.bacnetHexStringReceived()](./src/main/java/no/entra/bacnet/agent/observer/BlockingRecordAndForwardObserver.java)
The main class is [AgentDaemon](./src/main/java/no/entra/bacnet/agent/AgentDaemon.java)

### How to Run

```
mvn clean install
java -DDEVICE_CONNECTION_STRING="Hostname=....<from portal.azure.com>" -jar target/bacnet-ip-agent-*.jar 
```

## Docker

### Ubuntu

Update <your docker user> in all docker/*.sh files.

```
mvn clean install
docker/build-alpine-amd64.sh
docker/run-alpine.sh
```
### Raspberry PI


```
//TODO Support for Java 13
```
## Simulator

We provide a simulator for easy coding of the functionality you need. No need for working with a live Bacnet. 
The simulator is found in [BacnetUdpSimulator](https://github.com/entraeiendom/bacnet-ip-agent/blob/master/src/test/java/no/entra/bacnet/agent/simulators/BacnetUdpSimulator.java) .

Running the simulator is easy. Start the main method from your IDE. The simulator will then
put a WhoIs Bacnet message to your local net. 
If you want to run a specific Bacnet message, you may provide a HexString as the first program parameter.

### Simple Bacnet Simulator
Periodically send the same Bacnet Object message to your network.
[Simulator](https://github.com/entraeiendom/bacnet-client/blob/master/src/main/java/no/entra/bacnet/client/Simulator.java)

## Listen to Azure IoT hub

`az iot hub monitor-events --hub-name <your-hub-name>`
How? Look at https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest