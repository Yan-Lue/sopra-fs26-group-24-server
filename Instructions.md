# Instructions Postgres Local Setup

Note: Remote Setup done on Google Cloud via Cloud SQL --> check later

## Step by Step Instructions

Make sure to be connected to psql

1. Create database named "mydatabase":

`CREATE DATABASE mydatabase;`

2. Create User

`CREATE USER <env file user> WITH SUPERUSER;`

Password: For the password create a new ".env" file:

`ALTER USER <env file user> WITH PASSWORD '<check env file>';`

3. Now logout from psql

`\q`

4. Login as the new user

`psql -U <env file user> -d mydatabase -h localhost -W`

Password: chosen-password

5. Now finally create relations/tables

Users: `CREATE TABLE users (id SERIAL PRIMARY KEY, status VARCHAR(10) NOT NULL, token VARCHAR(255), username VARCHAR(100) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, bio TEXT);`

Guest Users: `CREATE TABLE guest_users (id SERIAL PRIMARY KEY, status VARCHAR(10) NOT NULL, token VARCHAR(255) NOT NULL, username VARCHAR(100) NOT NULL);`

## Additional Info PgAdmin4

Can be helpful to be used for verifaction and retrieval using a UI (--> replaces h2-console)

1. Add new Server (--> Query Tool Workspace --> "Welcome")
2. Server Name: "choose a name", Host Name/Address: localhost, Port: 5432, Database: mydatabase, user: check env file, password: check env file

To be extended for Google Cloud and Cloud SQL:

--> add respective credentials for Google Cloud
