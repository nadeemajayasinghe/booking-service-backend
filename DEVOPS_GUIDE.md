# DevOps Guide — Booking Service (Spring Boot)

> **Stack:** GitHub → GitHub Actions → Docker Hub → AWS ECS (Fargate) → Amazon RDS MySQL

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [LO1 — Version Control (GitHub)](#2-lo1--version-control-github)
3. [LO3 — Docker Containerisation](#3-lo3--docker-containerisation)
4. [Local Development with Docker Compose](#4-local-development-with-docker-compose)
5. [LO2 — CI/CD Pipeline (GitHub Actions)](#5-lo2--cicd-pipeline-github-actions)
6. [Container Registry — Docker Hub](#6-container-registry--docker-hub)
7. [LO4 — Deploy to AWS ECS (Fargate)](#7-lo4--deploy-to-aws-ecs-fargate)
8. [Security](#8-security)
9. [Verification Checklist](#9-verification-checklist)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Project Structure

```
booking-service-backend/
├── src/
│   └── main/
│       ├── java/com/carRental/booking/
│       └── resources/
│           └── application.properties   ← uses ${ENV_VARS}
├── Dockerfile                           ← multi-stage build
├── docker-compose.yml                   ← local dev stack
├── .dockerignore
├── .github/
│   └── workflows/
│       └── ci-cd.yml                    ← GitHub Actions pipeline
├── pom.xml
└── DEVOPS_GUIDE.md                      ← this file
```

---

## 2. LO1 — Version Control (GitHub)

### Make the repository public

1. Go to your repo on GitHub
2. **Settings** → scroll to **Danger Zone**
3. Click **Change visibility** → choose **Public** → confirm

### First push with all DevOps files

```bash
git add .
git commit -m "feat: add Docker, docker-compose, CI/CD pipeline, and DevOps guide"
git push origin main
```

> ⚠️ **Never commit passwords.** `application.properties` now uses `${DB_PASSWORD}` — real values live only in GitHub Secrets and AWS Parameter Store.

---

## 3. LO3 — Docker Containerisation

### Dockerfile — multi-stage build

| Stage | Base image | Purpose |
|-------|-----------|---------|
| **builder** | `eclipse-temurin:17-jdk-alpine` | Compiles source with Maven |
| **runtime** | `eclipse-temurin:17-jre-alpine` | Runs the JAR — no JDK, no Maven |

**Security baked into the image:**
- Non-root user (`appuser`) — prevents privilege escalation
- Minimal Alpine OS — smaller attack surface
- `/actuator/health` HEALTHCHECK — platform knows when the app is ready

### Build & run the image manually

```bash
# Build
docker build -t booking-service:local .

# Run (standalone, no DB)
docker run -p 8081:8081 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/vehicle_rental_booking?useSSL=false&serverTimezone=UTC" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="yourpassword" \
  -e EUREKA_ENABLED="false" \
  booking-service:local

# Test health endpoint
curl http://localhost:8081/actuator/health
```

---

## 4. Local Development with Docker Compose

`docker-compose.yml` spins up both the **Spring Boot app** and **MySQL 8** together with a single command.

```bash
# Start the full stack (builds app image automatically)
docker-compose up --build

# Run in the background
docker-compose up --build -d

# View application logs
docker-compose logs -f app

# Stop everything and remove volumes
docker-compose down -v
```

### Services started

| Service | Container | Port | Notes |
|---------|-----------|------|-------|
| MySQL 8 | `booking-mysql` | `3307` (host) → `3306` (container) | Mapped to 3307 to avoid conflict with local MySQL |
| Spring Boot | `booking-service` | `8081` | Waits for DB health check before starting |

### Test locally

```bash
# List all bookings
curl http://localhost:8081/api/bookings

# Health check
curl http://localhost:8081/actuator/health
```

---

## 5. LO2 — CI/CD Pipeline (GitHub Actions)

Pipeline file: `.github/workflows/ci-cd.yml`

### Pipeline overview

```
git push to main
      │
      ▼
┌─────────────────────┐
│  Job 1: Build+Test  │  runs on every push & PR
│  ./mvnw package     │
│  ./mvnw test        │
└────────┬────────────┘
         │ (passes)
         ▼
┌─────────────────────┐
│  Job 2: Docker Hub  │  main branch only
│  docker build       │
│  docker push :latest│
└────────┬────────────┘
         │ (pushed)
         ▼
┌─────────────────────┐
│  Job 3: AWS ECS     │  main branch only
│  update task def    │
│  rolling deploy     │
└─────────────────────┘
```

### Required GitHub Secrets

Go to: **repo → Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value |
|-------------|-------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub Access Token (see below) |
| `AWS_ACCESS_KEY_ID` | IAM user access key |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |
| `AWS_REGION` | e.g. `us-east-1` |
| `ECS_CLUSTER` | Name of your ECS cluster (e.g. `booking-cluster`) |
| `ECS_SERVICE` | Name of your ECS service (e.g. `booking-service`) |
| `ECS_TASK_DEFINITION` | Task definition family name (e.g. `booking-task`) |
| `ECS_CONTAINER_NAME` | Container name inside task def (e.g. `booking-service`) |

### Create Docker Hub Access Token

1. Log in to [hub.docker.com](https://hub.docker.com)
2. **Account Settings** → **Personal access tokens** → **Generate new token**
3. Give it a name like `github-actions`, scope: **Read & Write**
4. Copy the token → paste as `DOCKERHUB_TOKEN` secret in GitHub

---

## 6. Container Registry — Docker Hub

Your image is stored at:
```
docker.io/YOUR_DOCKERHUB_USERNAME/booking-service:latest
```

After the first successful pipeline run you can also pull it manually:
```bash
docker pull YOUR_DOCKERHUB_USERNAME/booking-service:latest
```

---

## 7. LO4 — Deploy to AWS ECS (Fargate)

### One-time AWS Setup

#### Step 1 — Create RDS MySQL database

```bash
# Create an RDS MySQL 8 instance (free-tier eligible)
aws rds create-db-instance \
  --db-instance-identifier booking-mysql \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --master-username dbadmin \
  --master-user-password "YourStr0ngPassword!" \
  --allocated-storage 20 \
  --db-name vehicle_rental_booking \
  --publicly-accessible \
  --backup-retention-period 7 \
  --region us-east-1

# Get the endpoint (run after a few minutes)
aws rds describe-db-instances \
  --db-instance-identifier booking-mysql \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

#### Step 2 — Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name booking-cluster \
  --region us-east-1
```

#### Step 3 — Create ECS Task Definition

Create a file `task-definition-template.json`:

```json
{
  "family": "booking-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::YOUR_ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "booking-service",
      "image": "YOUR_DOCKERHUB_USERNAME/booking-service:latest",
      "portMappings": [
        {
          "containerPort": 8081,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "DB_URL",
          "value": "jdbc:mysql://YOUR_RDS_ENDPOINT:3306/vehicle_rental_booking?useSSL=true&serverTimezone=UTC"
        },
        {
          "name": "DB_USERNAME",
          "value": "dbadmin"
        },
        {
          "name": "EUREKA_ENABLED",
          "value": "false"
        },
        {
          "name": "JPA_DDL_AUTO",
          "value": "update"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:ssm:us-east-1:YOUR_ACCOUNT_ID:parameter/booking/db-password"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "wget -qO- http://localhost:8081/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/booking-service",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

Register the task definition:
```bash
aws ecs register-task-definition \
  --cli-input-json file://task-definition-template.json \
  --region us-east-1
```

#### Step 4 — Store DB password securely in AWS SSM Parameter Store

```bash
aws ssm put-parameter \
  --name "/booking/db-password" \
  --value "YourStr0ngPassword!" \
  --type SecureString \
  --region us-east-1
```

#### Step 5 — Create an Application Load Balancer (ALB)

```bash
# Create ALB (via Console is easier — AWS Console → EC2 → Load Balancers → Create)
# Or use CLI:
aws elbv2 create-load-balancer \
  --name booking-alb \
  --subnets subnet-XXXX subnet-YYYY \
  --security-groups sg-XXXX \
  --region us-east-1

# Create target group
aws elbv2 create-target-group \
  --name booking-tg \
  --protocol HTTP \
  --port 8081 \
  --target-type ip \
  --vpc-id vpc-XXXX \
  --health-check-path /actuator/health \
  --region us-east-1
```

#### Step 6 — Create ECS Service

```bash
aws ecs create-service \
  --cluster booking-cluster \
  --service-name booking-service \
  --task-definition booking-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-XXXX,subnet-YYYY],securityGroups=[sg-XXXX],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:YOUR_ACCOUNT_ID:targetgroup/booking-tg/XXXX,containerName=booking-service,containerPort=8081" \
  --region us-east-1
```

### After setup — every git push auto-deploys! ✅

---

## 8. Security

| Layer | Mechanism |
|-------|-----------|
| **No secrets in code** | All passwords use `${ENV_VAR}` — real values only in GitHub Secrets & AWS SSM |
| **Non-root container** | Docker runs as `appuser` — limits blast radius if container is compromised |
| **HTTPS** | ALB handles TLS termination — configure an ACM certificate on the listener |
| **DB encryption** | RDS uses `useSSL=true` — data in transit encrypted |
| **DB password** | Stored in AWS SSM Parameter Store as `SecureString` (KMS-encrypted) |
| **CORS** | Restrict `allowedOrigins` in `CorsConfig.java` to your frontend domain only |
| **IAM least privilege** | GitHub Actions IAM user only has `ecs:*` and `ssm:GetParameters` — not admin |
| **Security groups** | RDS security group allows inbound MySQL only from ECS security group |

### Tighten CORS (recommended before production)

In `src/main/java/com/carRental/booking/config/CorsConfig.java`, change:
```java
// ❌ Too permissive:
configuration.setAllowedOrigins(List.of("*"));

// ✅ Production — use your actual frontend URL:
configuration.setAllowedOrigins(List.of("https://your-frontend-domain.com"));
```

---

## 9. Verification Checklist

After full deployment, verify each item:

- [ ] GitHub repo is **public**: `github.com/YOUR_USERNAME/booking-service-backend`
- [ ] GitHub Actions pipeline is **green**: repo → **Actions** tab
- [ ] Docker Hub image exists: `hub.docker.com/r/YOUR_USERNAME/booking-service`
- [ ] ECS service shows **RUNNING** tasks in AWS Console
- [ ] ALB DNS responds: `curl http://YOUR_ALB_DNS/actuator/health`
- [ ] HTTPS works (add ACM cert to ALB listener)
- [ ] API accessible: `curl https://YOUR_ALB_DNS/api/bookings`
- [ ] New push to `main` → pipeline runs → ECS auto-redeploys

---

## 10. Troubleshooting

| Problem | Fix |
|---------|-----|
| `./mvnw: Permission denied` in CI | Pipeline runs `chmod +x mvnw` before build |
| Docker push fails in CI | Verify `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets are set correctly |
| ECS task keeps restarting | Check CloudWatch logs: **ECS → Cluster → Service → Logs** |
| DB connection refused from ECS | Add inbound rule in RDS security group: MySQL (3306) from ECS security group |
| `Could not connect to Eureka` error | Ensure env var `EUREKA_ENABLED=false` is set in ECS task definition |
| `Access denied for user` DB error | Verify `DB_USERNAME` and `DB_PASSWORD` match what was set during RDS creation |
| Actuator health returns 503 | Check if `spring-boot-starter-actuator` is in `pom.xml` and app has started |

---

*Last updated: March 2026*
