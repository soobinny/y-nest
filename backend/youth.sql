CREATE SCHEMA IF NOT EXISTS youth;

USE youth;

-- =========================
-- users
-- =========================
CREATE TABLE users
(
    role        ENUM ('USER','ADMIN') NOT NULL DEFAULT 'USER',
    id          INT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255)          NOT NULL UNIQUE,
    password    VARCHAR(255)          NOT NULL,
    name        VARCHAR(255)          NOT NULL,
    age         INT,
    income_band VARCHAR(50),
    region      VARCHAR(50),
    is_homeless BOOLEAN                        DEFAULT FALSE,
    deleted     BOOLEAN               NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP             NULL     DEFAULT NULL,
    created_at  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================
-- products
-- =========================
CREATE TABLE products
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    type       ENUM ('HOUSING', 'FINANCE') NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    provider   VARCHAR(100),
    detail_url VARCHAR(500)
);

-- =========================
-- finance_companies
-- =========================
CREATE TABLE finance_companies
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    fin_co_no VARCHAR(20)  NOT NULL UNIQUE,
    name      VARCHAR(100) NOT NULL,
    homepage  VARCHAR(255),
    contact   VARCHAR(50)
);

-- =========================
-- finance_products
-- ========================
CREATE TABLE finance_products
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    product_id     INT                                                                           NOT NULL,
    fin_co_no      VARCHAR(20)                                                                   NOT NULL,
    product_type   ENUM ('DEPOSIT', 'SAVING', 'MORTGAGE_LOAN', 'RENT_HOUSE_LOAN', 'CREDIT_LOAN') NOT NULL,
    join_condition TEXT,
    interest_rate  DECIMAL(5, 2),
    min_deposit    INT,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    FOREIGN KEY (fin_co_no) REFERENCES finance_companies (fin_co_no) ON DELETE CASCADE
);

-- =========================
-- finance_loan_options
-- =========================
CREATE TABLE finance_loan_options
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    finance_product_id INT           NOT NULL,

    -- 기본 금리 정보
    lend_rate_min      DECIMAL(5, 2) NULL,
    lend_rate_max      DECIMAL(5, 2) NULL,
    lend_rate_avg      DECIMAL(5, 2) NULL,

    -- 대출 옵션 공통 필드
    rpay_type_name     VARCHAR(100)  NULL, -- 상환유형 이름 (예: 원리금균등, 만기일시)
    lend_type_name     VARCHAR(100)  NULL, -- 금리유형 이름 (예: 고정, 변동)
    mrtg_type_name     VARCHAR(100)  NULL, -- 담보유형 이름 (예: 아파트, 보증 등)

    -- 신용등급별 금리 필드 추가
    crdt_lend_rate_type     VARCHAR(10)   NULL COMMENT '금리구분 코드',
    crdt_lend_rate_type_nm  VARCHAR(100)  NULL COMMENT '금리구분명 (고정/변동)',
    crdt_grad_1             DECIMAL(5, 2) NULL COMMENT '900점 초과',
    crdt_grad_4             DECIMAL(5, 2) NULL COMMENT '801~900점',
    crdt_grad_5             DECIMAL(5, 2) NULL COMMENT '701~800점',
    crdt_grad_6             DECIMAL(5, 2) NULL COMMENT '601~700점',
    crdt_grad_10            DECIMAL(5, 2) NULL COMMENT '501~600점',
    crdt_grad_11            DECIMAL(5, 2) NULL COMMENT '401~500점',
    crdt_grad_12            DECIMAL(5, 2) NULL COMMENT '301~400점',
    crdt_grad_13            DECIMAL(5, 2) NULL COMMENT '300점 이하',
    crdt_grad_avg           DECIMAL(5, 2) NULL COMMENT '평균 금리',

    -- 생성/수정 시각
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- FK
    CONSTRAINT fk_flo_product
        FOREIGN KEY (finance_product_id) REFERENCES finance_products (id) ON DELETE CASCADE,

    -- 유효성 제약
    CONSTRAINT chk_flo_rate_range CHECK (
        lend_rate_min IS NULL OR lend_rate_max IS NULL OR lend_rate_min <= lend_rate_max
        ),
    CONSTRAINT chk_flo_rate_bounds CHECK (
        (lend_rate_min IS NULL OR (lend_rate_min BETWEEN 0 AND 30.00)) AND
        (lend_rate_max IS NULL OR (lend_rate_max BETWEEN 0 AND 30.00)) AND
        (lend_rate_avg IS NULL OR (lend_rate_avg BETWEEN 0 AND 30.00)) AND
        (crdt_grad_avg IS NULL OR (crdt_grad_avg BETWEEN 0 AND 30.00))
        )
);

-- 조회 인덱스
CREATE INDEX idx_flo_product ON finance_loan_options (finance_product_id);
CREATE INDEX idx_flo_avg_rate ON finance_loan_options (lend_rate_avg);
CREATE INDEX idx_flo_types_name ON finance_loan_options (lend_type_name, rpay_type_name, mrtg_type_name);
CREATE INDEX idx_flo_crdt_avg ON finance_loan_options (crdt_grad_avg);

-- =========================
-- housing_announcements
-- =========================
CREATE TABLE housing_announcements
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    product_id  INT NOT NULL UNIQUE,
    region_name VARCHAR(100),
    notice_date DATE,
    close_date  DATE,
    status      VARCHAR(30),
    category    VARCHAR(30),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_housing_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- =========================
-- favorites
-- =========================
CREATE TABLE favorites
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uq_fav_user_product UNIQUE (user_id, product_id) -- 중복 즐겨찾기 방지
);
CREATE INDEX idx_fav_user ON favorites (user_id);
CREATE INDEX idx_fav_product ON favorites (product_id);

-- =========================
-- notifications
-- =========================
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

-- =========================
-- password_reset_tokens
-- =========================
CREATE TABLE password_reset_tokens
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    token      VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prt_userid (user_id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
