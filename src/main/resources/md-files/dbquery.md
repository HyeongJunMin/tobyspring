### create table users
CREATE TABLE users ( 
   id VARCHAR(50) PRIMARY KEY , 
   name VARCHAR(50) NOT NULL, 
   password VARCHAR(50) NOT NULL,
   level int NOT NULL,
   login int NOT NULL,
   recommend int NOT NULL,
   email VARCHAR(50) NOT NULL
);