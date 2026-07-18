-- V3: Team 4 — results and academic records

CREATE TABLE results (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT       NOT NULL,
    course_code  VARCHAR(50)  NOT NULL,
    term         VARCHAR(50)  NOT NULL,
    grade        VARCHAR(10)  NOT NULL,
    score        DOUBLE       NOT NULL,
    credits      INT          NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_results_student ON results (student_id);
CREATE INDEX idx_results_course ON results (course_code);
CREATE UNIQUE INDEX idx_results_unique_submission ON results (student_id, course_code, term);
