Here's the README for the "Hermes" project in English:

---

# Hermes

Hermes is a project designed for managing account balances and frozen balances, with a focus on ensuring synchronization and consistency in a distributed environment. The system is built to handle user balance changes safely, preventing race conditions.

## Key Features

- **Balance and Freeze Balance Management**: Provides operations for managing account balances and frozen balances.
- **Ensures Synchronization in Distributed Environments**: Uses Redis locks and event sourcing to ensure that user balance changes are processed by a single thread at any given time.
- **Processes Operations from Kafka**: Hermes receives account operations from Kafka and applies them to the respective accounts.

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
   git clone https://github.com/hungnm98/hermes.git
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

Let me know if you need any adjustments!
