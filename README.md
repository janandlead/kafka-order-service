# Kafka Order Service

Producer-only Spring Boot 3 / Java 21 application. `POST /publish` saves an order in PostgreSQL and asynchronously publishes the saved `Order` as JSON to Kafka topic `orders`.

## Run locally

Create the database:

```sql
CREATE DATABASE kafka_order_db;
```

Create the topic:

```bash
kafka-topics.sh --create --topic orders --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

Start PostgreSQL and Kafka, then run:

```bash
mvn spring-boot:run
```

Swagger UI is available at `http://localhost:8081/swagger-ui.html`.

Example request:

```json
{
  "productCode": "P1001",
  "productName": "Laptop",
  "quantity": 2,
  "price": 50000
}
```

Consume events:

```bash
kafka-console-consumer.sh --topic orders --from-beginning --bootstrap-server localhost:9092
```

The database save and Kafka publish are separate operations. Kafka publishing is asynchronous, so the HTTP response can return before broker acknowledgment. This implementation does not provide atomic delivery across PostgreSQL and Kafka: a later Kafka failure can leave a saved database row without an event. PostgreSQL or Kafka outages instead fail the corresponding operation and should be handled with operational monitoring and, for stronger guarantees, a transactional outbox pattern.
