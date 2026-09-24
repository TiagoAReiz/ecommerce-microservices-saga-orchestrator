-- Executed once by the official postgres image on first start (empty volume).
-- Database-per-service: each microservice owns its schema and its own Flyway history.
CREATE DATABASE cart_db;
CREATE DATABASE delivery_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE products_db;
CREATE DATABASE users_db;
CREATE DATABASE gateway_db;
