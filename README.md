# Hermes

Hermes is a high-performance project designed for managing account balances and frozen balances, focusing on synchronization and consistency in distributed environments. The system is built to handle user balance changes safely, preventing race conditions.

## Key Features

- **Balance and Freeze Balance Management**: Provides operations for managing account balances and frozen balances.
- **Ensures Synchronization in Distributed Environments**: Uses Redis locks and event sourcing to ensure that user balance changes are processed by a single thread at any given time.
- **Processes Operations from Kafka**: Hermes receives account operations from Kafka and applies them to the respective accounts.

## Performance Benchmarks

- **Environment**:
   - **Hardware**: MacBook M1 Pro with 32GB RAM
   - **Setup**:
      - 1 node Kafka
      - 1 node Redis
      - 1 node MongoDB
      - 1 node Hermes

- **Load Details**:
   - **Total Accounts**: 100,000
   - **Operations per Account**: 10
   - **Total Operations**: 1,000,000

- **Performance Metrics**:
   - **Average Response Time**: 1 second
   - **Response Time Distribution**: 70% of requests complete in under 300 milliseconds, ensuring high responsiveness under load.

## Architecture and Implementation

- **Redis Lock**: Ensures thread safety in a multi-threaded environment, preventing race conditions.
- **Event Sourcing**: Stores events of account changes to maintain a history and the current state of each account.
- **Kafka**: Serves as the source of account operations and distributes them to processing threads in the system.

## Installation Guide

### System Requirements

- Java 21
- Redis
- Kafka
- MongoDB

### Setup

1. **Clone the Project**:
   ```bash
   git clone https://github.com/username/hermes.git
   cd hermes
   ```

2. **Configure**: Update the configuration files to connect to Kafka, Redis, and MongoDB.

3. **Run the Project**:
   ```bash
   ./mvnw spring-boot:run
   ```

## Usage

Hermes will automatically listen for operations from Kafka and handle account transactions accordingly. All user balance changes are strictly managed to ensure safety in multi-threaded processing environments.

## Contributions

Contributions for performance improvements and feature enhancements are welcome. Please open a pull request or an issue to start contributing.

---

Let me know if there's anything else to add!
