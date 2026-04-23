package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 422);
        error.put("error", "Unprocessable Entity");
        error.put("message", "The referenced resource does not exist: '" + ex.getFieldName() + "' with value '" + ex.getFieldValue() + "' was not found.");
        error.put("field", ex.getFieldName());
        error.put("value", ex.getFieldValue());

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
