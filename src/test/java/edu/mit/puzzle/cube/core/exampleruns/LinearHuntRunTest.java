package edu.mit.puzzle.cube.core.exampleruns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import edu.mit.puzzle.cube.core.Cube2Application;
import edu.mit.puzzle.cube.core.db.ConnectionFactory;
import edu.mit.puzzle.cube.core.db.InMemoryConnectionFactory;
import edu.mit.puzzle.cube.core.events.CompositeEventProcessor;
import edu.mit.puzzle.cube.core.events.CoreEventFactory;
import edu.mit.puzzle.cube.core.events.EventFactory;
import edu.mit.puzzle.cube.core.model.HuntStatusStore;
import edu.mit.puzzle.cube.core.model.SubmissionStore;
import edu.mit.puzzle.cube.core.model.VisibilityStatusSet;
import edu.mit.puzzle.cube.core.serverresources.*;
import edu.mit.puzzle.cube.huntimpl.linearexample.LinearExampleUnlockEventProcessor;
import edu.mit.puzzle.cube.modules.events.SetToSolvedOnCorrectSubmission;
import edu.mit.puzzle.cube.modules.model.StandardVisibilityStatusSet;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.spring.SpringRouter;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LinearHuntRunTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Cube2Application cube2Application;

    @Before
    public void setup() throws Exception {
        cube2Application = new Cube2Application();
        cube2Application.start();
    }

    private JsonNode getAllSubmissions() throws IOException {
        Request request = new Request(Method.GET, "/submissions");
        Response response = cube2Application.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("submissions"));
        assertTrue(responseJson.get("submissions").isArray());
        return responseJson;
    }

    private JsonNode getVisibility(String teamId, String puzzleId) throws IOException {
        Request request = new Request(Method.GET, String.format("/visibilities/%s/%s",teamId,puzzleId));
        Response response = cube2Application.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    private JsonNode postHuntStart(String runId) throws IOException {
        String json = String.format("{'eventType':'HuntStart','runId':'%s'}", runId);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/events", representation);
        Response response = cube2Application.handle(request);
        assertEquals(200, response.getStatus().getCode());
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    private JsonNode postFullRelease(String runId, String puzzleId) throws IOException {
        String json = String.format("{'eventType':'FullRelease','runId':'%s','puzzleId':'%s'}", runId, puzzleId);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/events", representation);
        Response response = cube2Application.handle(request);
        assertEquals(200, response.getStatus().getCode());
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("processed"));
        assertTrue(responseJson.get("processed").asBoolean());
        return responseJson;
    }

    private JsonNode postNewSubmission(String teamId, String puzzleId, String submission) throws IOException {
        String json = String.format("{'teamId':'%s','puzzleId':'%s','submission':'%s'}",
                teamId, puzzleId, submission);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST, "/submissions", representation);
        Response response = cube2Application.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("created"));
        assertEquals(true, responseJson.get("created").asBoolean());
        return responseJson;
    }

    private JsonNode getSubmission(Integer submissionId) throws IOException {
        Request request = new Request(Method.GET, String.format("/submissions/%d",submissionId));
        Response response = cube2Application.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("status"));
        assertTrue(responseJson.get("status").isTextual());
        return responseJson;
    }

    private JsonNode postUpdateSubmission(Integer submissionId, String status) throws IOException {
        String json = String.format("{'status':'%s'}", status);
        Representation representation = new JsonRepresentation(json);
        Request request = new Request(Method.POST,
                String.format("/submissions/%d", submissionId),
                representation);
        Response response = cube2Application.handle(request);
        JsonNode responseJson = MAPPER.readTree(response.getEntityAsText());
        assertTrue(responseJson.has("updated"));
        assertEquals(true, responseJson.get("updated").asBoolean());
        return responseJson;
    }

    @Test
    public void testSubmittingAndUnlock() throws IOException {
        JsonNode json = getAllSubmissions();
        assertEquals(0, json.get("submissions").size());

        json = getVisibility("testerteam","puzzle1");
        assertEquals("INVISIBLE", json.get("status").asText());
        json = getVisibility("testerteam","puzzle2");
        assertEquals("INVISIBLE", json.get("status").asText());

        postHuntStart("development");

        json = getVisibility("testerteam","puzzle1");
        assertEquals("UNLOCKED", json.get("status").asText());
        json = getVisibility("testerteam","puzzle2");
        assertEquals("INVISIBLE", json.get("status").asText());

        postNewSubmission("testerteam", "puzzle1", "guess");

        json = getSubmission(1);
        assertEquals("SUBMITTED", json.get("status").asText());
        json = getAllSubmissions();
        assertEquals(1, json.get("submissions").size());

        postUpdateSubmission(1, "CORRECT");

        json = getVisibility("testerteam", "puzzle1");
        assertEquals("SOLVED", json.get("status").asText());
        json = getVisibility("testerteam", "puzzle2");
        assertEquals("UNLOCKED", json.get("status").asText());

        json = getVisibility("testerteam", "puzzle5");
        assertEquals("INVISIBLE", json.get("status").asText());

        postFullRelease("development", "puzzle5");
        json = getVisibility("testerteam", "puzzle5");
        assertEquals("UNLOCKED", json.get("status").asText());
    }

}
