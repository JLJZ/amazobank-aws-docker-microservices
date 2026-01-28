# Set-up Local Development Environment

## Step 1: Create an `application-dev.properties` file in `src/main/resources/` with the following content
``` ini
# JDBC connection string to the local MySQL database
spring.datasource.url=jdbc:mysql://localhost:3306/user_db

# Database username for development
spring.datasource.username=cs301

# Database password for development
spring.datasource.password=cs301

# Fully qualified class name for MySQL JDBC driver
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Maximum number of connections in the Hikari connection pool
spring.datasource.hikari.maximum-pool-size=10

# Hibernate DDL mode: 'update' automatically updates DB schema to match entity class
spring.jpa.hibernate.ddl-auto=update

# Show SQL statements in logs (useful for debugging in dev)
spring.jpa.show-sql=true

# SQL initialization mode: always run scripts in development
spring.sql.init.mode=always

# Port for the Spring Boot server
server.port=8080

# Address the server will listen on (0.0.0.0 allows external access)
server.address=0.0.0.0

# URL of the frontend app for local development to allow CORS
frontend.url=http://localhost:3000

# JWT secret for signing tokens (dev only)
security.jwt.secret=HARDCODED_SECRET_FOR_DEV_ONLY

```

## Step 2: Set-up a local database
``` SQL
CREATE USER IF NOT EXISTS 'cs301'@'%' IDENTIFIED BY 'cs301';
GRANT ALL PRIVILEGES ON `user_db`.* to 'cs301'@'%';

DROP DATABASE IF EXISTS user_db;
CREATE DATABASE user_db;

FLUSH PRIVILEGES;
```

## Step 3: Run the Springboot application
For Windows:
``` powershell
.\gradlew.bat bootRun --args='--spring-active-profile=dev'
```

For Mac/Linux:
``` shell
./gradlew bootRun --args='--spring-active-profile=dev'
```
> [!NOTE]
> MacOS users may need to first run `chmod u+x ./gradlew` prior to running the above command.


## Step 4: Create an Admin account
Using Postman:
```
HTTP Method: POST
HTTP URL: http://localhost:8080/users

Request Body
=============
firstName: John
lastName: Doe
email: johndoe@amazobank.com
password: P@ssw0rd
role: Admin
```

Using CLI:
``` SHELL
curl -X POST \
-H "Content-type: application/json" \
-d '{
	"firstName": "John",
	"lastName": "Doe",
	"email": "johndoe@amazobank.com",
	"password": "P@ssw0rd",
	"role": "Admin"
}' \
http://localhost:8080/api/users
```

## Step 5: Log in to `http://localhost:3000`
```
Email: johndoe@amazobank.com
Password: P@ssw0rd
```