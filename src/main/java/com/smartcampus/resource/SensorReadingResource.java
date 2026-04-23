package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings — get all readings
    // ----------------------------------------------------------------
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        List<SensorReading> readings = store.getReadings()
                .getOrDefault(sensorId, new ArrayList<>());

        return Response.ok(readings).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings — add a new reading
    // ----------------------------------------------------------------
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Block readings if sensor is in MAINTENANCE status
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // Validate reading
        if (reading == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Reading body is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Generate ID and timestamp if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Store the reading
        store.getReadings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // Side effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Reading added successfully.");
        response.put("sensorId", sensorId);
        response.put("updatedCurrentValue", sensor.getCurrentValue());
        response.put("reading", reading);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
