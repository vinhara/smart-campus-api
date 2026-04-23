# Smart Campus Sensor & Room Management API
A RESTful API built with JAX-RS (Jersey 2.32) and Apache Tomcat 9 for managing university campus rooms and sensors.

## API Overview
This API provides a comprehensive interface for managing the Smart Campus infrastructure. It is built using JAX-RS (Jersey 2.32) deployed on Apache Tomcat 9. All data is stored in-memory using ConcurrentHashMap data structures — no database is used.
The API is versioned under /api/v1 and exposes three core resource collections:

Resource           Path
Discovery          GET /api/v1
Rooms              /api/v1/rooms
Sensors            /api/v1/sensors
Sensor Readings    /api/v1/sensors/{sensorId}/readings

## Project structure 

smartcampus/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── application/
    │   └── SmartCampusApplication.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableException.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── ApiLoggingFilter.java


##Requirements

Java 11 or higher
Maven 3.6 or higher
Apache Tomcat 9.x (Jersey 2.32 is NOT compatible with Tomcat 10)
NetBeans IDE (recommended) or any IDE with Tomcat support

## How to Build and Run

1. Clone the repository
git clone https://github.com/vinhara/smart-campus-api.git

cd smartcampus-api

2. Open the project in NetBeans

File → Open Project → select the smartcampus folder

3. Make sure Apache Tomcat 9 is added in NetBeans

Go to Services tab → right-click Servers → Add Server → Apache Tomcat or TomEE
Browse to your extracted apache-tomcat-9.x.x folder
Enter a username and password → Finish

4. Build the project

Right-click project → Clean and Build

5. Run the project

Right-click project → Run
Tomcat will start automatically and deploy the app

6. The API is now available at:
http://localhost:8080/api/v1/

## Sample curl Commands

1. Get API discovery info
curl http://localhost:8080/api/v1/

2. Create a room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'

3. Get all rooms
curl http://localhost:8080/api/v1/rooms

4. Create a sensor linked to a room
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'

5. Filter sensors by type
curl http://localhost:8080/api/v1/sensors?type=Temperature

6. Post a sensor reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.5}'

7. Get all readings for a sensor
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings

8. Delete a room (fails if sensors are assigned)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301


## Report: Answers to Coursework Questions

Part 1: Service Architecture & Setup 

Question 1.1

Question: In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

By default, JAX-RS creates a brand new instance of every resource class for each incoming HTTP request. This is known as per-request scope and is the default behaviour mandated by the JAX-RS specification. Each resource class instance is created when a request arrives and discarded once the response is sent. This means instance fields are never shared between requests and are inherently thread-safe in isolation.
However, this architectural decision has a direct impact on how in-memory data must be managed. Since each request gets its own resource class instance, storing data as instance fields would cause all data to be lost the moment a request completes. To persist data across requests, a shared structure must exist outside the resource class lifecycle.
In this implementation, a singleton DataStore class holds all data in ConcurrentHashMap instances. Because multiple threads can serve simultaneous requests each with their own resource class instance and all of them access the same singleton, thread safety is essential. ConcurrentHashMap is used instead of a standard HashMap because it supports safe concurrent read and write operations without throwing ConcurrentModificationException or causing data corruption. Had a plain HashMap been used, two simultaneous POST requests could corrupt the internal state, resulting in data loss or unpredictable behaviour.



Question 1.2

Question: Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?
HATEOAS (Hypermedia as the Engine of Application State) is a REST principle where API responses include navigational links that guide clients to related resources and actions. Instead of constructing URLs manually, clients interact with the API by following these links.It is considered a hallmark of advanced RESTful design because it makes the API self-describing and dynamically navigable. Each response not only provides data but also indicates the possible next actions (state transitions), allowing the client to move through the system without prior knowledge of all endpoints.
The main benefit for client developers is decoupling from the API’s internal URL structure. If endpoint paths change, a HATEOAS-compliant client continues to function correctly by following the updated links in responses, without requiring code changes. In contrast, static documentation requires clients to hardcode URLs, making them fragile and prone to breaking when the API evolves.Additionally, HATEOAS improves usability by allowing developers to explore the API dynamically through embedded links, reducing reliance on external documentation.In this implementation, the GET /api/v1 discovery endpoint includes a links section that provides navigation to primary resources, demonstrating the HATEOAS principle.

Part 2: Room Management

Question 2.1 

Question: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.
When returning a list of rooms, the choice between returning only IDs versus full room objects involves a trade-off between network efficiency and client-side complexity.
Returning only IDs produces a minimal payload and reduces bandwidth consumption, which is beneficial at scale with thousands of rooms. However, this forces the client to make one additional GET request per room to retrieve its details known as the N+1 request problem. For a list of 500 rooms, this would mean 501 HTTP requests, which is highly inefficient and significantly increases latency.
Returning full room objects in a single response allows the client to render all data immediately with no follow-up requests, greatly reducing latency and simplifying client-side logic. The trade-off is a larger payload. For most use cases especially in a campus management dashboard where all room details are needed  returning full objects is the superior approach. A best practice middle ground is to return a summary object containing the most commonly used fields while omitting heavy nested data. This implementation returns full room objects to maximise client usability in a single request.




Question 2.2 

Question: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.
In this implementation, the DELETE operation is idempotent with respect to server state, but not with respect to the HTTP response code.
The first DELETE request on a room that exists and has no sensors will successfully remove it and return 200 OK. Any subsequent DELETE request for the same room ID will return 404 Not Found, since the room no longer exists in the data store.From the perspective of REST semantics, this is the standard and widely accepted behaviour. The state of the server is idempotent  after any number of DELETE calls for the same room ID, the room remains absent. The response code changes, but the resource state does not. Strictly speaking, some REST purists argue that a truly idempotent DELETE should return 404 on the first call too, but returning 200 on success and 404 on repeat calls is the industry-standard convention and is acceptable under the HTTP specification.

Part 3: Sensor Operations & Linking 

Question 3.1

Question: We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

When a method is annotated with @Consumes(MediaType.APPLICATION_JSON), it indicates that the endpoint only accepts requests with a Content-Type of application/json.
If a client sends a request with a different content type, such as text/plain or application/xml, JAX-RS will automatically return an HTTP 415 Unsupported Media Type response. This occurs before the resource method is invoked.

At runtime, JAX-RS inspects the Content-Type header and attempts to find a suitable MessageBodyReader capable of converting the request body into a Java object. If no compatible reader is found for the given media type, the framework rejects the request immediately with a 415 error.



This behaviour is beneficial because it enforces a strict contract between the client and server, ensures that the resource method only receives valid and expected data formats, and eliminates the need for manual content-type validation within the method. It also provides clear, standards-compliant feedback to the client about the required format.



Question 3.2 

Question:Youimplementedthisfilteringusing@QueryParam.Contrastthiswithanalterna- tive design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?

Using @QueryParam for filtering (e.g., GET /api/v1/sensors?type=CO2) is generally superior to embedding filters in the URL path (e.g., /api/v1/sensors/type/CO2) for several reasons.

First, query parameters are optional by design. The base endpoint /api/v1/sensors remains valid and returns all sensors when no filters are applied. In contrast, a path-based approach makes the filter appear mandatory and part of the resource structure.

Second, query parameters are easily composable. Multiple filters can be combined in a flexible and scalable way, such as ?type=CO2&status=ACTIVE. Achieving the same with path segments would require defining numerous route variations, making the API harder to maintain and extend.

Third, RESTful design distinguishes between resource identity and query modifiers. Path segments are intended to identify specific resources (e.g., /sensors/TEMP-001), while query parameters are used to refine or filter collections. Embedding filter values like CO2 in the path incorrectly suggests a hierarchical sub-resource rather than a filtering criterion.

Overall, query parameters provide a more flexible, scalable, and semantically correct approach for filtering and searching collections in RESTful APIs.








Part 4: Deep Nesting with Sub – Resources

Question 4.1 

Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive con- troller class?

The Sub-Resource Locator pattern delegates responsibility for handling nested URL paths to dedicated resource classes, rather than defining all logic within a single controller. In this implementation, the SensorResource class delegates all paths under /{sensorId}/readings to a separate SensorReadingResource class, instead of embedding reading-related logic directly within the sensor resource.

The primary architectural benefit is separation of concerns. Each class has a single, well-defined responsibility ,SensorResource manages sensors, while SensorReadingResource handles reading history. This improves modularity, making each class smaller, more focused, and easier to understand, test, and maintain independently.

This approach also significantly reduces complexity. In large APIs with deep nesting such as /buildings/{id}/floors/{id}/rooms/{id}/sensors/{id}/readings/{id} placing all logic in a single monolithic controller would result in a large, difficult-to-maintain class with many tightly coupled methods. By delegating logic to sub-resources, the codebase remains structured and manageable.

Furthermore, the pattern improves scalability and extensibility. New nested resources can be added by introducing new resource classes and locator methods, without modifying existing classes. This aligns with the Open/Closed Principle, where the system is open for extension but closed for modification.

Overall, sub-resource locators promote a clean, modular, and scalable architecture that is well-suited for large and evolving RESTful APIs.







Part 5: Advanced Error Handling, Exception Mapping & Logging 


Question 5.2

Question: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

When a client sends a POST request to create a sensor with a roomId value that does not exist, returning 404 Not Found would be semantically misleading. A 404 status communicates that the requested URL was not found  but the URL /api/v1/sensors is perfectly valid and was found by the server.
The actual problem is that the request body is syntactically valid JSON, but it fails business logic validation because it references a resource that does not exist. HTTP 422 Unprocessable Entity is specifically designed for this scenario: the request was well-formed, the content type was correct, and the server understood the request  but it could not process it due to a semantic error in the payload. This communicates far more precisely to the client developer that the issue lies in the data sent rather than the URL used, making the API significantly easier to consume and debug.

 Question 5.4 

Question: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

Exposing raw Java stack traces to external API consumers poses a significant security risk because it reveals sensitive internal implementation details of the system.
Firstly, stack traces disclose the internal package and class structure (e.g., com.smartcampus.store.DataStore), effectively providing an attacker with a blueprint of the application architecture. They may also expose specific library names and versions (e.g., Jersey, Jackson), enabling attackers to identify known vulnerabilities (CVEs) associated with those dependencies and craft targeted exploits.
Additionally, stack traces include exact file names and line numbers where errors occur, significantly reducing the effort required to locate and exploit weaknesses in the code. In some cases, they may also reveal sensitive operational details such as file system paths, configuration data, or fragments of database queries.
Overall, a stack trace acts as a form of unintended information disclosure, giving attackers valuable reconnaissance data that would otherwise require significant effort to obtain.
The correct approach is to log detailed stack traces securely on the server side while returning a generic, user-friendly error message (e.g., HTTP 500) to the client, thereby preventing leakage of sensitive information.

Question 5.5

Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single re- source method?

Using JAX-RS filters for logging is superior to inserting Logger.info() statements inside every resource method because filters handle logging as a cross-cutting concern in a centralized and consistent way.

First, filters promote the DRY (Don’t Repeat Yourself) principle. Logging logic is defined in a single class, so any changes to the log format or additional fields only need to be implemented once, rather than across multiple resource methods. This greatly improves maintainability.

Second, filters are applied automatically to every request and response, ensuring complete and consistent coverage. Manual logging is error-prone, as developers may forget to add logging statements in new endpoints, leading to gaps in observability.

Third, filters can operate on both the request and response lifecycle. This allows logging of important information such as HTTP method, URI, headers, response status codes, and even timing metrics, providing a more comprehensive view of API behaviour.

Finally, using filters keeps resource classes clean and focused on business logic. By separating logging into a dedicated component, the codebase follows the principle of separation of concerns, improving readability and reducing cognitive complexity. This approach is also extensible, as the same filter mechanism can be used for other cross-cutting concern=