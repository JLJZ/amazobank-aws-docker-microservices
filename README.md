# Cloud-Native CRM System (AWS)

## Overview

This repository showcases the **architecture, cloud design, and DevOps practices** behind a **cloud-native CRM system** built on AWS.
The system is designed to manage client interactions, streamline banking operations, and improve profitability while meeting enterprise-grade requirements for **scalability, security, availability, and maintainability**.

---

## Business Requirements

### Product Vision

A **CRM System** that enables banking staff to manage client data, accounts, and transactions through a secure, web-based interface.

### Key Characteristics

- Cloud-Native Architecture (AWS)
- Web-Based Frontend
- Microservice Backend
- External System Integration (SFTP)
- Secure Authentication & Authorization
- High Availability & Fault Tolerance

---

## High-Level Architecture

- **Frontend**: Web-based UI served via CloudFront
- **Backend**: Microservices + Serverless (ECS Fargate, AWS Lambda)
- **Authentication**: Amazon Cognito (OAuth2 / OIDC)
- **Data Layer**: Aurora RDS, DynamoDB
- **Integration**: External SFTP via Lambda
- **CI/CD**: GitHub Actions → AWS (ECS, Lambda, CloudFront)

---

## Stakeholders & Access Model

### Business Operations

| Stakeholder | Description | Permissions |
| ---------- | ----------- | ----------- |
| Client | Bank customer whose data is stored in the CRM | N/A |
| CRM User (Agent) | Banking staff managing assigned clients | Read/Write: Client & Account Services<br>Read-only: Transaction Logs (own clients) |
| Root Administrator | Manages CRM System Administrators | Create/Delete Admins<br>Read-only: System Logs |
| Administrator (Admin) | Manages CRM Users (Agents) | Read/Write: User Service<br>Read-only: System Logs |

---

### IT Operations

| Stakeholder | Responsibilities | Permissions |
| ---------- | ---------------- | ----------- |
| Application Developers | Develop & deploy microservices | Deploy to ECS/Lambda/CloudFront via CI/CD<br>Read-only CloudWatch logs |
| Database Administrators | Manage databases, backups, availability | Create/backup Aurora RDS & DynamoDB<br>No access to data |
| Security Engineers | Enforce security & compliance | IAM, KMS, GuardDuty, CloudTrail |
| AWS Cloud Engineers | Provision & manage cloud infrastructure | Create all AWS resources |

---

## Architecturally Significant Requirements

### AWS Lambda – LLM Service

Triggered via **ALB POST requests**.

#### Supported Requests

```json
{ "action": "generate", "prompt": "<prompt>" }
```

```json
{ "action": "send", "recipient": "<email>", "subject": "<subject>", "body": "<body>" }
```

---

## Maintainability & Design Principles

### SOLID Principles

- Builder Pattern for flexible domain object construction
- Clear separation of concerns

### Microservice Architecture

- Client, Account, Transaction, Logging services
- Independent scaling and deployment
- Stateless, event-driven design

### DevOps Practices

- CI/CD with GitHub Actions
- Automated testing and deployments
- Serverless-first approach to reduce operational overhead

---

## Availability & Resilience

- Stateless ECS services behind ALB / API Gateway
- Multi-AZ Aurora RDS with automatic failover
- JWT-based authentication (no sticky sessions)
- Cognito-managed identity and access

---

## Security Architecture

- VPC-isolated deployment
- Least-privilege IAM roles
- RBAC enforced at backend
- Secrets managed outside source code
- Centralized, secure logging

---

## Performance Optimizations

- Lambda container image caching (ECR)
- CloudFront edge caching
- S3-backed artifact storage for faster provisioning

---

## Summary

This project demonstrates **enterprise-grade cloud architecture**, **secure system design**, and **modern DevOps workflows** suitable for scalable, regulated environments.
