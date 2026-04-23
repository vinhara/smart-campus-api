package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ----------------------------------------------------------------
    // GET /api/v1/rooms — list all rooms
    // ----------------------------------------------------------------
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    // ----------------------------------------------------------------
    // POST /api/v1/rooms — create a new room
    // ----------------------------------------------------------------
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Room ID is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (store.getRooms().containsKey(room.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "A room with ID '" + room.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // ----------------------------------------------------------------
    // GET /api/v1/rooms/{roomId} — get a specific room
    // ----------------------------------------------------------------
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Room with ID '" + roomId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        return Response.ok(room).build();
    }

    // ----------------------------------------------------------------
    // TEST ONLY — remove after recording video
    // ----------------------------------------------------------------
    

    // ----------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId} — delete a room (only if no sensors)
    // ----------------------------------------------------------------
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        // Idempotent: if room doesn't exist, return 404
        if (room == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Room with ID '" + roomId + "' was not found.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Safety check: block deletion if sensors are still assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.getRooms().remove(roomId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Room '" + roomId + "' has been successfully deleted.");
        response.put("roomId", roomId);

        return Response.ok(response).build();
    }
}
