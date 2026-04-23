package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 403);
        error.put("error", "Forbidden");
        error.put("message", "Sensor '" + ex.getSensorId() + "' cannot accept new readings. Current status: " + ex.getStatus() + ". Only ACTIVE sensors can receive readings.");
        error.put("sensorId", ex.getSensorId());
        error.put("currentStatus", ex.getStatus());

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
