# NexEvent Application

NexEvent is a Spring Boot application designed to get nearby events, update preferences, and get event recommendations using a MySQL database and Redis for caching.

**The application is deployed on AWS EC2 and can be accessed at: [http://ec2-3-140-233-85.us-east-2.compute.amazonaws.com:8080/](http://ec2-3-140-233-85.us-east-2.compute.amazonaws.com:8080/).**

This guide will help you set up and run the application using Docker and Docker Compose.

## Prerequisites

Before you begin, ensure you have the following installed on your machine:

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Setup Instructions

### 1. Clone the Repository

Clone the repository to your local machine:

```bash
git clone https://github.com/ZiY1/nex-event-backend.git
cd nex-event-backend
```

### 2. Create a .env file

Create a .env file in the root of the project with the following variables:

```dotenv
TICKETMASTER_API_KEY=your_ticketmaster_api_key
JWT_SECRET=your_jwt_secret
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/nexevent_db?serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=nexevent_db
MYSQL_USER=root
MYSQL_PASSWORD=root
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

#### How to Obtain API Keys

- **TICKETMASTER_API_KEY**: 
  1. Visit the [Ticketmaster Developer Portal](https://developer.ticketmaster.com/).
  2. Sign up for an account or log in if you already have one.
  3. Create a new application to obtain your API key.

- **JWT_SECRET**: 
  - You can generate a secure JWT secret using a command line tool like `openssl`:
    ```bash
    openssl rand -base64 32
    ```
  - Alternatively, you can use an online generator or any method that provides a strong, random string.

### 3. Run Docker Compose

Run the following command to start the application:

```bash
docker-compose up --build
```

This will start the application, MySQL database, and Redis container. The application backend will be available at `http://localhost:8080`.

### 4. Access the Application

For your convenience, I have deployed the frontend of the application on GitHub Pages. Open your web browser and navigate to [NexEvent application](https://ziy1.github.io/nex-event-frontend). You should see the NexEvent application.

### 5. Stop the Application

To stop the application, use the following command:

```bash
docker-compose down
```

# EC2 Deployment Guide

## Initial Setup

### 1. Deploy Application to EC2
```bash
# Copy JAR file to EC2
scp -i /path/to/nexevent-key.pem target/nexevent-0.0.1-SNAPSHOT.jar ubuntu@<EC2_PUBLIC_IP>:/home/ubuntu

# SSH into EC2
ssh -i /path/to/nexevent-key.pem ubuntu@<EC2_PUBLIC_IP>
```

### 2. Environment Setup
```bash
# Load environment variables
export $(cat .env | xargs)

# Set proper permissions for .env
chmod 600 /home/ubuntu/.env
chown ubuntu:ubuntu /home/ubuntu/.env
```

## Database Configuration

### 1. MySQL Setup
```sql
# Access MySQL
mysql -u root -p
# or
sudo mysql

# Configure root user
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
FLUSH PRIVILEGES;

# Create database
CREATE DATABASE nexevent_db;
SHOW DATABASES;
```

### 2. MySQL Performance Configuration
```ini
# Edit MySQL configuration
sudo nano /etc/mysql/my.cnf

[mysqld]
# General Settings
user = mysql
skip-name-resolve

# Memory Optimization
innodb_buffer_pool_size = 64M
key_buffer_size = 8M
innodb_log_buffer_size = 4M
max_connections = 10
thread_cache_size = 2
table_open_cache = 64
tmp_table_size = 8M
max_heap_table_size = 8M

# Logging
log_error = /var/log/mysql/error.log
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-queries.log
long_query_time = 1

# Storage Engine
default_storage_engine = InnoDB
innodb_file_per_table = 1
innodb_flush_log_at_trx_commit = 2
innodb_redo_log_capacity = 64M
innodb_flush_method = O_DIRECT

# Timeouts
wait_timeout = 600
interactive_timeout = 600
connect_timeout = 10

# Caching and Temporary Files
tmpdir = /tmp
max_allowed_packet = 16M
```

```bash
# Restart MySQL to apply changes
sudo systemctl restart mysql.service
```

## System Optimization

### 1. Swap Space Configuration
```bash
# Create and configure swap
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Optimize swap usage
sudo sysctl vm.swappiness=10
echo "vm.swappiness=10" | sudo tee -a /etc/sysctl.conf
```

### 2. Service Optimization
```bash
# Disable unnecessary services
sudo systemctl stop snapd
sudo systemctl disable snapd
sudo systemctl mask snapd

# Disable Amazon SSM Agent if not needed
sudo systemctl stop amazon-ssm-agent
sudo systemctl disable amazon-ssm-agent
```

## Application Deployment

### 1. Create Systemd Service
```bash
# Create service file
sudo nano /etc/systemd/system/nexevent.service
```

```ini
[Unit]
Description=NexEvent Spring Boot Application
After=network.target

[Service]
Type=simple
WorkingDirectory=/home/ubuntu
EnvironmentFile=/home/ubuntu/.env
ExecStart=/usr/bin/java -jar /home/ubuntu/nexevent-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5
StandardOutput=file:/home/ubuntu/nexevent.log
StandardError=file:/home/ubuntu/nexevent-error.log
User=ubuntu
Group=ubuntu

[Install]
WantedBy=multi-user.target
```

### 2. Configure Automatic Restarts
```bash
# Edit crontab
sudo crontab -e

# Add restart schedule (runs at 1 AM and 1 PM)
0 1,13 * * * systemctl restart nexevent && logger "NexEvent app restarted by cron at $(date)"

# Verify cron job
sudo crontab -l
```

## Monitoring and Maintenance

### 1. System Monitoring
```bash
# Monitor memory usage
ps aux --sort=-%mem | head -n 10
free -m
watch free -m

# Monitor swap usage
free -m
```

### 2. Application Monitoring
```bash
# Check application logs
tail -f /home/ubuntu/nexevent.log
sudo journalctl -u nexevent

# Check service status
sudo systemctl status nexevent
```

### 3. Cache Management
```bash
# Clear Redis cache
redis-cli --scan --pattern "ticketmaster*" | xargs -d '\n' redis-cli DEL
```

### 4. Process Management
```bash
# List Java processes
ps aux | grep 'java'

# Kill specific process
kill <PROCESS_ID>
```

## Frontend Deployment

### Update Static Files
```bash
# Clear existing static files
rm -rf ../nex-event-backend/src/main/resources/static/*

# Move new build files
mv build/* ../nex-event-backend/src/main/resources/static
```

## Quick Reference

### Manual Application Start
```bash
nohup java -jar nexevent-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### Service Management
```bash
# Reload service configuration
sudo systemctl daemon-reload

# Restart service
sudo systemctl restart nexevent

# Check service status
sudo systemctl status nexevent
```


