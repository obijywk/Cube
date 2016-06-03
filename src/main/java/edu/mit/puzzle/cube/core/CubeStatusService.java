package edu.mit.puzzle.cube.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.engine.application.CorsResponseHelper;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.service.CorsService;
import org.restlet.service.StatusService;

public class CubeStatusService extends StatusService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @AutoValue
    public abstract static class JsonStatus {
        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder setCode(int code);
            public abstract Builder setDescription(String description);
            public abstract JsonStatus build();
        }

        public static Builder builder() {
            return new AutoValue_CubeStatusService_JsonStatus.Builder();
        }

        @JsonProperty("code") public abstract int getCode();
        @JsonProperty("description") public abstract String getDescription();
    }

    private final CorsResponseHelper corsResponseHelper;

    public CubeStatusService(CorsService corsService) {
        // The CorsService post-processing doesn't seem to happen when exceptions are thrown, but
        // the headers it adds are necessary, so we'll add the required response headers ourselves
        // in error cases.
        //
        // TODO: try to find a cleaner way to set this up - ideally, we could do this with a
        // separate service or filter somewhere, and CubeStatusService wouldn't need to be aware
        // of CORS.
        corsResponseHelper = new CorsResponseHelper();
        corsResponseHelper.allowAllRequestedHeaders = corsService.isAllowingAllRequestedHeaders();
        corsResponseHelper.allowedCredentials = corsService.isAllowedCredentials();
        corsResponseHelper.allowedHeaders = corsService.getAllowedHeaders();
        corsResponseHelper.allowedOrigins = corsService.getAllowedOrigins();
        corsResponseHelper.exposedHeaders = corsService.getExposedHeaders();
    }

    @Override
    public Status toStatus(Throwable throwable, Resource resource) {
        corsResponseHelper.addCorsResponseHeaders(resource.getRequest(), resource.getResponse());
        if (throwable instanceof ResourceException) {
            return toStatus(throwable.getCause());
        }
        return toStatus(throwable);
    }

    @Override
    public Status toStatus(Throwable throwable, Request request, Response response) {
        corsResponseHelper.addCorsResponseHeaders(request, response);
        return toStatus(throwable);
    }

    private Status toStatus(Throwable throwable) {
        int code = 500;
        if (throwable instanceof AuthenticationException) {
            code = 401;
        } else if (throwable instanceof AuthorizationException) {
            code = 403;
        }
        return new Status(code, throwable, throwable.getMessage());
    }

    @Override
    public Representation toRepresentation(Status status, Request request, Response response) {
        try {
            return new JsonRepresentation(MAPPER.writeValueAsString(JsonStatus.builder()
                    .setCode(status.getCode())
                    .setDescription(status.getReasonPhrase())
                    .build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
