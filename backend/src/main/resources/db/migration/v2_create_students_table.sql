CREATE TABLE students (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_number  VARCHAR(50)  NOT NULL UNIQUE,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    department      VARCHAR(100) NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    user_id         BIGINT       NOT NULL UNIQUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_students_department ON students (department);
CREATE INDEX idx_students_full_name ON students (full_name);