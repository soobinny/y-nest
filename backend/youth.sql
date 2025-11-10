CREATE SCHEMA IF NOT EXISTS youth;

USE youth;

-- =========================
-- users
-- =========================
CREATE TABLE users
(
    role                 ENUM ('USER','ADMIN')        NOT NULL DEFAULT 'USER',
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    email                VARCHAR(255)                 NOT NULL UNIQUE,
    password             VARCHAR(255)                 NOT NULL,
    name                 VARCHAR(255)                 NOT NULL,
    age                  INT,
    income_band          VARCHAR(50),
    region               VARCHAR(50),
    is_homeless          BOOLEAN                               DEFAULT FALSE,
    deleted              BOOLEAN                      NOT NULL DEFAULT FALSE,
    deleted_at           TIMESTAMP                    NULL     DEFAULT NULL,
    created_at           TIMESTAMP                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP                    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notification_enabled BOOLEAN                      NOT NULL DEFAULT TRUE,
    notification_channel ENUM ('EMAIL','KAKAO','SMS') NOT NULL DEFAULT 'EMAIL'
    birthdate DATE NOT NULL;
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
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    finance_product_id     INT           NOT NULL,

    -- 기본 금리 정보
    lend_rate_min          DECIMAL(5, 2) NULL,
    lend_rate_max          DECIMAL(5, 2) NULL,
    lend_rate_avg          DECIMAL(5, 2) NULL,

    -- 이전 금리 이력 백업 컬럼
    prev_lend_rate_min     DECIMAL(5, 2) NULL COMMENT '이전 최소 금리',
    prev_lend_rate_max     DECIMAL(5, 2) NULL COMMENT '이전 최대 금리',
    prev_lend_rate_avg     DECIMAL(5, 2) NULL COMMENT '이전 평균 금리',

    -- 대출 옵션 공통 필드
    rpay_type_name         VARCHAR(100)  NULL, -- 상환유형 이름 (예: 원리금균등, 만기일시)
    lend_type_name         VARCHAR(100)  NULL, -- 금리유형 이름 (예: 고정, 변동)
    mrtg_type_name         VARCHAR(100)  NULL, -- 담보유형 이름 (예: 아파트, 보증 등)

    -- 신용등급별 금리 필드 추가
    crdt_lend_rate_type    VARCHAR(10)   NULL COMMENT '금리구분 코드',
    crdt_lend_rate_type_nm VARCHAR(100)  NULL COMMENT '금리구분명 (고정/변동)',
    crdt_grad_1            DECIMAL(5, 2) NULL COMMENT '900점 초과',
    crdt_grad_4            DECIMAL(5, 2) NULL COMMENT '801~900점',
    crdt_grad_5            DECIMAL(5, 2) NULL COMMENT '701~800점',
    crdt_grad_6            DECIMAL(5, 2) NULL COMMENT '601~700점',
    crdt_grad_10           DECIMAL(5, 2) NULL COMMENT '501~600점',
    crdt_grad_11           DECIMAL(5, 2) NULL COMMENT '401~500점',
    crdt_grad_12           DECIMAL(5, 2) NULL COMMENT '301~400점',
    crdt_grad_13           DECIMAL(5, 2) NULL COMMENT '300점 이하',
    crdt_grad_avg          DECIMAL(5, 2) NULL COMMENT '평균 금리',

    -- 생성/수정 시각
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

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
-- LH housing_announcements
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

CREATE TABLE lh_notices
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    upp_ais_tp_nm VARCHAR(100),                    -- 공고유형명 (예: 임대공고, 분양공고)
    ais_tp_cd_nm  VARCHAR(100),                    -- 세부유형명
    pan_nm        VARCHAR(255),                    -- 공고명
    cnp_cd_nm     VARCHAR(100),                    -- 지역명
    pan_ss        VARCHAR(50),                     -- 공고상태
    pan_nt_st_dt  VARCHAR(50),                     -- 공고게시일
    clsg_dt       VARCHAR(50),                     -- 공고마감일
    dtl_url       VARCHAR(500),                    -- 상세URL
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lh_notice (pan_nm, pan_nt_st_dt) -- 중복방지
);

-- =========================
-- SH housing_announcements
-- =========================
CREATE TABLE sh_announcements
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    source         VARCHAR(255) NOT NULL,
    external_id    VARCHAR(255) NOT NULL,
    title          VARCHAR(255),
    department     VARCHAR(255),
    post_date      DATE,
    views          INT,
    recruit_status VARCHAR(20),
    supply_type    VARCHAR(255),
    category       VARCHAR(50),
    content_html   LONGTEXT,
    attachments    JSON,
    region         VARCHAR(50),
    detail_url     VARCHAR(255),
    crawled_at     DATETIME,
    updated_at     DATETIME,
    UNIQUE KEY uq_sh_source_external_id (source, external_id)
);

-- =========================
-- youth_policies
-- =========================
CREATE TABLE youth_policies
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_no       VARCHAR(50)  NOT NULL UNIQUE,
    policy_name     VARCHAR(255) NOT NULL,
    description     MEDIUMTEXT,
    keyword         VARCHAR(255),
    category_large  VARCHAR(100),
    category_middle VARCHAR(100),
    agency          VARCHAR(255),
    apply_url       VARCHAR(500),
    region_code     TEXT,
    target_age      VARCHAR(50),
    support_content MEDIUMTEXT,
    start_date      VARCHAR(100),
    end_date        VARCHAR(100),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_policy_region (region_code(100)),
    INDEX idx_policy_category (category_large, category_middle)
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
    product_id INT NULL,
    type       ENUM ('EMAIL', 'KAKAO', 'SMS'),
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
