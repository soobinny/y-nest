CREATE DATABASE IF NOT EXISTS youth;
CREATE SCHEMA IF NOT EXISTS youth;

USE youth;

CREATE TABLE users
(
    role ENUM('USER','OWNER') NOT NULL DEFAULT 'USER',
    id          INT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    age         INT,
    income_band VARCHAR(50),
    region      VARCHAR(50),
    is_homeless BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE products
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    type       ENUM ('HOUSING', 'FINANCE') NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    provider   VARCHAR(100),
    detail_url VARCHAR(500)
);

CREATE TABLE housing_announcements
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT NOT NULL,
    region_name VARCHAR(100),
    notice_date DATE,
    close_date  DATE,
    status      VARCHAR(50),
    category    VARCHAR(100),
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE finance_companies
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    fin_co_no VARCHAR(20)  NOT NULL UNIQUE,
    name      VARCHAR(100) NOT NULL,
    homepage  VARCHAR(255),
    contact   VARCHAR(50)
);

CREATE TABLE finance_products
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    product_id     INT                        NOT NULL,
    fin_co_no      VARCHAR(20)                NOT NULL,
    product_type   ENUM ('DEPOSIT', 'SAVING') NOT NULL,
    join_condition TEXT,
    interest_rate  DECIMAL(5, 2),
    min_deposit    INT,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    FOREIGN KEY (fin_co_no) REFERENCES finance_companies (fin_co_no) ON DELETE CASCADE
);

CREATE TABLE favorites
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE notifications
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    type       ENUM ('EMAIL', 'PUSH'),
    status     ENUM ('SENT', 'FAILED'),
    message    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);