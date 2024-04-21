# Northeastern University REST API Prototype Model (INFO 7255)

## Project Overview
This repository contains development work for a REST API prototype model, completed as part of the INFO 7255 course at Northeastern University. The project focuses on building a REST API to parse a JSON schema model and includes three prototype demos.

## Prototype Demo 1
- Developed a Spring Boot-based REST API to parse a sample JSON schema.
- Implemented CRUD operations (GET, POST, DELETE).
- Stored JSON schema in a Redis key-value store.

## Prototype Demo 2
- Expanded functionality with additional operations (PUT, PATCH).
- Implemented security measures using JWT or OAuth2.

## Prototype Demo 3
- Enhanced capabilities by integrating Elasticsearch.
- Utilized RedisSMQ for REST API queuing.

## Prerequisites
- Java
- Maven
- Redis Server
- Elasticsearch and Kibana (Local or cloud-based)

## Build and Run
1. Run as a Spring Boot Application in any IDE.
2. **Query Elasticsearch**:
   - Run both the applications, FinalProject and Consumer Message Queue (CMQ). CMQ application will create the indexes.
   - Execute POST queries from Postman.
   - Run custom search queries as per your use case (Some examples are provided in DemoQueries).
   - (Optional) For testing purposes, to test the indexes separately, run the PUT query in Testing-ElasticSearchQueries on Kibana. This will create an index in Elasticsearch.
