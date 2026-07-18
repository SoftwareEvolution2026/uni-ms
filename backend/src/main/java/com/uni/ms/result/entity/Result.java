package com.uni.ms.result.entity;

import com.uni.ms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "results")
public class Result extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Integer credits;
}
