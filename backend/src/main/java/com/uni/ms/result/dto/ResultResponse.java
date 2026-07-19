package com.uni.ms.result.dto;

import com.uni.ms.result.entity.Result;

public record ResultResponse(
        Long id,
        Long studentId,
        String courseCode,
        String term,
        String grade,
        Double score,
        Integer credits
) {

    public static ResultResponse from(Result result) {
        return new ResultResponse(
                result.getId(),
                result.getStudentId(),
                result.getCourseCode(),
                result.getTerm(),
                result.getGrade(),
                result.getScore(),
                result.getCredits()
        );
    }
}
