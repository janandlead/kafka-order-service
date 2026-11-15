# AKHQ setup for the existing Kafka broker

This guide documents the working local setup and the fix for the `orders` topic not appearing in AKHQ.

## 1. Check the existing containers

Run these commands in PowerShell with Docker Desktop running:

```powershell
docker ps --format '{{.Names}} {{.Image}} {{.Ports}}'
```

The setup uses:

| Component | Container / location | Connection |
| --- | --- | --- |
| Kafka | `dazzling_lehmann`, image `apache/kafka:4.4.0` | Windows: `localhost:9092` |
| AKHQ | `akhq-akhq-1`, image `tchiotludo/akhq:latest` | Browser: `http://127.0.0.1:8080/` |
| AKHQ Compose file | `E:\Kafka\akhq\compose.yaml` | Kafka: `order-kafka:29092` |
| Order service | This project | Kafka: `localhost:9092` |

Replace the container names in subsequent commands if yours differ. These steps configure the existing broker; they do not create another Kafka broker.

## 2. Verify that the topic exists

```powershell
docker exec dazzling_lehmann /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec dazzling_lehmann /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders
```

`orders` already exists in this setup. If setting up a new broker where it does not exist, create it:

```powershell
docker exec dazzling_lehmann /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic orders --partitions 1 --replication-factor 1
```

## 3. Understand the connection problem

The original AKHQ configuration used `host.docker.internal:9092`. This let AKHQ contact the broker initially, but Kafka advertised `localhost:9092` in its metadata. Inside the AKHQ container, `localhost` refers to AKHQ itself, so subsequent broker connections failed.

AKHQ logs showed:

```text
Connection to node 1 (localhost/127.0.0.1:9092) could not be established.
```

The fix uses two broker listeners:

| Listener | Advertised address | Used by |
| --- | --- | --- |
| `PLAINTEXT` | `localhost:9092` | Applications running on Windows |
| `INTERNAL` | `order-kafka:29092` | AKHQ on the Docker network |

Port `29092` does not need to be published to Windows because AKHQ connects directly through the Docker network.

## 4. Put Kafka and AKHQ on the same Docker network

The existing Compose project is named `akhq` and uses the network `akhq_default`.

```powershell
docker network inspect akhq_default
docker inspect dazzling_lehmann --format '{{json .NetworkSettings.Networks}}'
```

If `akhq_default` does not exist yet, create it:

```powershell
docker network create akhq_default
```

If Kafka is not already attached to it, connect Kafka with the alias used by the internal listener:

```powershell
docker network connect --alias order-kafka akhq_default dazzling_lehmann
```

This connection has already been made in the current setup. Do not repeat it when the endpoint already exists.

## 5. Save the Kafka listener configuration

The current image reads user-provided configuration from `/mnt/shared/config/server.properties` during startup. That directory is backed by a Docker volume.

For the first setup, copy the existing effective configuration into that directory. If the destination already exists, back it up and review it before replacing it.

```powershell
docker exec dazzling_lehmann ls -la /mnt/shared/config
docker exec dazzling_lehmann cp /opt/kafka/config/server.properties /mnt/shared/config/server.properties
```

Update the three listener properties:

```powershell
docker exec dazzling_lehmann sed -i -e 's@^listeners=.*@listeners=PLAINTEXT://:9092,CONTROLLER://:9093,INTERNAL://:29092@' -e 's@^advertised.listeners=.*@advertised.listeners=PLAINTEXT://localhost:9092,INTERNAL://order-kafka:29092@' -e 's@^listener.security.protocol.map=.*@listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL,INTERNAL:PLAINTEXT@' /mnt/shared/config/server.properties
```

The resulting properties must be:

```properties
listeners=PLAINTEXT://:9092,CONTROLLER://:9093,INTERNAL://:29092
advertised.listeners=PLAINTEXT://localhost:9092,INTERNAL://order-kafka:29092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL,INTERNAL:PLAINTEXT
```

Preserve all other broker properties, including the controller configuration and `log.dirs`.

In this setup, a live update with `kafka-configs.sh` was rejected with `Cannot update these configs dynamically: [advertised.listeners]`. Apply the saved configuration by restarting the existing container:

```powershell
docker restart --timeout 30 dazzling_lehmann
```

Kafka is briefly unavailable during this restart.

## 6. Configure AKHQ

Use the following contents for `E:\Kafka\akhq\compose.yaml`:

```yaml
name: akhq

services:
  akhq:
    image: tchiotludo/akhq:latest
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      AKHQ_CONFIGURATION: |
        akhq:
          connections:
            local-kafka:
              properties:
                bootstrap.servers: "order-kafka:29092"
```

For a new setup where you manually created the network in step 4, append this declaration so Compose uses that existing network:

```yaml
networks:
  default:
    external: true
    name: akhq_default
```

The current setup already has a Compose-managed network, so that additional declaration is optional. A backup of the original Compose file was saved as `E:\Kafka\akhq\compose.yaml.before-listener-fix` during the fix.

Start or update AKHQ:

```powershell
docker compose -f E:/Kafka/akhq/compose.yaml up -d
```

## 7. Verify the result

Check Kafka directly:

```powershell
docker exec dazzling_lehmann /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders
```

Check the same topic through AKHQ:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/local-kafka/topic -TimeoutSec 20 | ConvertTo-Json -Depth 8
```

The response should include `orders` and its leader address `order-kafka:29092`. Immediately after this fix, the response reported one partition and four messages; the message count will change as events are published.

Open **http://127.0.0.1:8080/**, select **local-kafka**, and open **Topics → orders**. Refresh the page if an earlier connection error is still displayed.

The order service keeps these settings in `src/main/resources/application.properties`:

```properties
spring.kafka.bootstrap-servers=localhost:9092
app.kafka.topic=orders
```

## 8. Troubleshoot or restart later

```powershell
docker logs --tail 100 akhq-akhq-1
docker logs --tail 100 dazzling_lehmann
docker network inspect akhq_default
docker compose -f E:/Kafka/akhq/compose.yaml ps
```

If logs still show connections to `localhost:9092` from AKHQ, verify the broker's effective listener configuration:

```powershell
docker exec dazzling_lehmann /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type brokers --entity-name 1 --describe --all | Select-String 'listeners=|listener.security.protocol.map='
```

If `order-kafka` cannot be resolved, check that both containers belong to `akhq_default` and Kafka has the `order-kafka` network alias.

To stop and start AKHQ without removing its network:

```powershell
docker compose -f E:/Kafka/akhq/compose.yaml stop
docker compose -f E:/Kafka/akhq/compose.yaml start
```

### Preserve existing data

The broker currently stores its Kafka logs at `/tmp/kraft-combined-logs` inside the existing container. Restarting that container preserves those files. Removing and recreating the broker container can lose them, even though other directories have Docker volumes. Back up or migrate that log directory to persistent storage before replacing the broker container.

The listener configuration in `/mnt/shared/config` survives restarts through its volume. If Kafka is recreated later, reattach the correct configuration and data storage and reconnect the Docker network alias.
