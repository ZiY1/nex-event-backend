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
