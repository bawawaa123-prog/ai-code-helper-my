CREATE DATABASE IF NOT EXISTS ai_code_helper
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ai_code_helper;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_account VARCHAR(64) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_name VARCHAR(64) DEFAULT '新用户',
    avatar_url VARCHAR(512) DEFAULT NULL,
    user_role VARCHAR(32) DEFAULT 'user',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_account (user_account)
);

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) DEFAULT '新会话',
    last_message VARCHAR(512) DEFAULT NULL,
    message_count INT DEFAULT 0,
    use_rag TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    KEY idx_user_id_update_time (user_id, update_time)
);

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    rag_enabled TINYINT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'success',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    KEY idx_session_id_create_time (session_id, create_time),
    KEY idx_user_id_create_time (user_id, create_time)
);

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    KEY idx_user_id_update_time (user_id, update_time)
);

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64) DEFAULT NULL,
    file_path VARCHAR(512) DEFAULT NULL,
    file_size BIGINT DEFAULT 0,
    segment_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    KEY idx_base_id_update_time (knowledge_base_id, update_time),
    KEY idx_user_id_update_time (user_id, update_time)
);

CREATE TABLE IF NOT EXISTS knowledge_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    token_count INT DEFAULT 0,
    vector_id VARCHAR(128) DEFAULT NULL,
    metadata TEXT DEFAULT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    KEY idx_document_id_segment_index (document_id, segment_index),
    KEY idx_base_id_document_id (knowledge_base_id, document_id)
);
