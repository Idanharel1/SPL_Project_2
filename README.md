---

# Project 2: Java Concurrency & Multithreading (Vacuum Robot Simulation)

## Overview

This project simulates a vacuum robot equipped with multiple sensors (LiDAR, camera, pose service) operating concurrently. The system processes sensor data in real-time to map objects in a room using multithreading.

## Key Features

- **Multithreading & Concurrency**: Implemented Java threads for parallel data processing.
- **Event-Driven Architecture**: Used a message-passing system for inter-thread communication.
- **Sensor Fusion**: Integrated LiDAR, camera, and pose data for a comprehensive environmental map.
- **Synchronization & Locking**: Ensured thread-safe operations with Java synchronization mechanisms.

## Technologies Used

- Java
- Multithreading & Synchronization
- JSON Parsing
- Event-Driven Programming

## Installation & Usage

```sh
mvn compile
mvn exec:java -Dexec.mainClass="com.vacuumrobot.Main"
```

