CREATE TABLE envelope_print_input (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    execution_id VARCHAR(100) NOT NULL,

    customer_id BIGINT NOT NULL,

    stamp VARCHAR(100),

    honorific VARCHAR(50),

    font_family VARCHAR(100),

    font_size INT,

    envelope_type VARCHAR(20),

    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,

    INDEX idx_envelope_input_execution_id (
        execution_id
    )
);