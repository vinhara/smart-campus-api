package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 409);
        error.put("error", "Conflict");
        error.put("message", "Room '" + ex.getRoomId() + "' cannot be deleted because it still has active sensors assigned to it. Please remove or reassign all sensors first.");
        error.put("roomId", ex.getRoomId());

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
