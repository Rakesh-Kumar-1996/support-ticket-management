package com.supporttickets.api;

import java.util.List;

public record ApiError(
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors
) {
}
