package com.emmanuelfinance.shared.aspect;

import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DeletedFilterAspect {

    private final EntityManager entityManager;

    @Around("@annotation(withDeletedFilter")
    public Object applyFilter(ProceedingJoinPoint joinPoint, WithDeletedFilter withDeletedFilter) throws Throwable {
        Session session = entityManager.unwrap(Session.class);

        if (withDeletedFilter.enabled()) {
            session.enableFilter("deletedFilter");
        } else {
            session.disableFilter("deletedFilter");
        }

        try {
            return joinPoint.proceed();
        } finally {
            session.disableFilter("deletedFilter");
        }
    }
}
