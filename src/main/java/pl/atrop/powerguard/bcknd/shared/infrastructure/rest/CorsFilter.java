package pl.atrop.powerguard.bcknd.shared.infrastructure.rest;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Configuration sources, in this order:
     *
     * 1. JVM property:
     *    -Dcors.allowed.origins=...
     *
     * 2. Environment variable:
     *    CORS_ALLOWED_ORIGINS=...
     *
     * Example:
     * http://localhost:8082,https://xpowerguard.pl
     */
    private static final String PROP = "cors.allowed.origins";
    private static final String ENV = "CORS_ALLOWED_ORIGINS";

    /**
     * Development defaults used only when no explicit configuration exists.
     */
    private static final List<Pattern> DEFAULT_ALLOWED = List.of(
            Pattern.compile("^https?://localhost(:\\d+)?$"),
            Pattern.compile("^https?://127\\.0\\.0\\.1(:\\d+)?$")
    );

    private static final AllowedOrigins ALLOWED = AllowedOrigins.load();

    @Override
    public void filter(ContainerRequestContext request) {

        // Browser CORS preflight.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            String origin = request.getHeaderString("Origin");

            Response.ResponseBuilder response = Response.ok();

            applyCorsHeaders(response, origin);

            request.abortWith(response.build());
        }
    }

    @Override
    public void filter(
            ContainerRequestContext request,
            ContainerResponseContext response) {

        String origin = request.getHeaderString("Origin");

        applyCorsHeaders(response, origin);
    }

    private void applyCorsHeaders(
            ContainerResponseContext response,
            String origin) {

        if (origin != null && ALLOWED.isAllowed(origin)) {
            response.getHeaders().putSingle(
                    "Access-Control-Allow-Origin",
                    origin
            );
        }

        response.getHeaders().putSingle(
                "Vary",
                "Origin"
        );

        response.getHeaders().putSingle(
                "Access-Control-Allow-Credentials",
                "true"
        );

        response.getHeaders().putSingle(
                "Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization, X-Requested-With"
        );

        response.getHeaders().putSingle(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, PATCH, OPTIONS"
        );

        response.getHeaders().putSingle(
                "Access-Control-Max-Age",
                "600"
        );
    }

    private void applyCorsHeaders(
            Response.ResponseBuilder response,
            String origin) {

        if (origin != null && ALLOWED.isAllowed(origin)) {
            response.header(
                    "Access-Control-Allow-Origin",
                    origin
            );
        }

        response.header(
                "Vary",
                "Origin"
        );

        response.header(
                "Access-Control-Allow-Credentials",
                "true"
        );

        response.header(
                "Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization, X-Requested-With"
        );

        response.header(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, PATCH, OPTIONS"
        );

        response.header(
                "Access-Control-Max-Age",
                "600"
        );
    }

    private static final class AllowedOrigins {

        private final Set<String> exact = new HashSet<>();
        private final List<Pattern> patterns = new ArrayList<>();

        static AllowedOrigins load() {

            AllowedOrigins allowed = new AllowedOrigins();

            String raw = System.getProperty(PROP);

            if (raw == null || raw.isBlank()) {
                raw = System.getenv(ENV);
            }

            if (raw != null && !raw.isBlank()) {

                for (String item : raw.split(",")) {

                    String value = item.trim();

                    if (value.isEmpty()) {
                        continue;
                    }

                    if (value.contains("*")) {
                        allowed.patterns.add(
                                wildcardToPattern(value)
                        );
                    } else {
                        allowed.exact.add(value);
                    }
                }

                return allowed;
            }

            allowed.patterns.addAll(DEFAULT_ALLOWED);

            return allowed;
        }

        boolean isAllowed(String origin) {

            if (exact.contains(origin)) {
                return true;
            }

            for (Pattern pattern : patterns) {
                if (pattern.matcher(origin).matches()) {
                    return true;
                }
            }

            return false;
        }

        private static Pattern wildcardToPattern(String value) {

            String[] parts = value.split("\\*", -1);

            StringBuilder regex = new StringBuilder("^");

            for (int i = 0; i < parts.length; i++) {

                if (i > 0) {
                    regex.append(".*");
                }

                regex.append(Pattern.quote(parts[i]));
            }

            regex.append("$");

            return Pattern.compile(regex.toString());
        }
    }
}