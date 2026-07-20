package com.uni.ms.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** Records audit events; inject into any module: record(email, "STUDENT_CREATED", detail). */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional
    public void record(String actorEmail, String action, String detail) {
        repository.save(new AuditLog(actorEmail, action, detail));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(String actorEmail, String action, String detail) {
        repository.save(new AuditLog(actorEmail, action, detail));
    }
}
