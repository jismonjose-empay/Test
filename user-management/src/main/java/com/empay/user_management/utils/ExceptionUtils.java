package com.empay.user_management.utils;

public final class ExceptionUtils {
    private ExceptionUtils() {}

    public static String extractConstraintName(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
