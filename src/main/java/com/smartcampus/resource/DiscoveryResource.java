package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("api",     "Smart Campus Sensor & Room Management API");
        meta.put("version", "1.0.0");
        meta.put("contact", "admin@smartcampus.ac.uk");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        meta.put("resources", resources);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        meta.put("_links", links);

        return Response.ok(meta).build();
    }
}
