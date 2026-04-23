package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ----------------------------------------------------------------
    // GET /api/v1/sensors — list all sensors, optionally filter by type
    // ----------------------------------------------------------------
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/sensors — register a new sensor
    // ----------------------------------------------------------------
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Sensor ID is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Check for duplicate sensor ID
        if (store.getSensors().containsKey(sensor.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "A sensor with ID '" + sensor.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Validate that the referenced roomId exists
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "roomId is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        Room room = store.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }

        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Save sensor
        store.getSensors().put(sensor.getId(), sensor);

        // Link sensor ID to the room
        room.getSensorIds().add(sensor.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sensor created successfully.");
        response.put("sensor", sensor);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // ----------------------------------------------------------------
    // Sub-resource locator: /api/v1/sensors/{sensorId}/readings
    // Delegates to SensorReadingResource for all reading operations
    // ----------------------------------------------------------------
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // ----------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId} — get a specific sensor
    // ----------------------------------------------------------------
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        return Response.ok(sensor).build();
    }
}
