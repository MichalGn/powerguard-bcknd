package pl.atrop.powerguard.bcknd.system.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.time.LocalDate;
import pl.atrop.powerguard.bcknd.auth.application.SiteAccessGuard;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.domain.UserRole;
import pl.atrop.powerguard.bcknd.auth.domain.UsersService;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.measurement.infrastructure.mqtt.PowerguardMqttSubscriber;
import pl.atrop.powerguard.bcknd.site.domain.SitesService;
import pl.atrop.powerguard.bcknd.system.application.EdgeConfigurationRegistry;
import pl.atrop.powerguard.bcknd.system.application.EdgeHealthRegistry;
import pl.atrop.powerguard.bcknd.system.application.GetSystemStatusUseCase;
import pl.atrop.powerguard.bcknd.system.domain.CommunicationStatusService;
import pl.atrop.powerguard.bcknd.shared.infrastructure.BuildInfo;

@Path("system")
public class SystemResource {
    @Inject private GetSystemStatusUseCase getSystemStatusUseCase;
    @Inject private EdgeHealthRegistry edgeHealthRegistry;
    @Inject private EdgeConfigurationRegistry edgeConfigurationRegistry;
    @Inject private PowerguardMqttSubscriber mqtt;
    @Inject private AuthTokenService tokens;
    @Inject private UsersService users;
    @Inject private SiteAccessGuard access;
    @Inject private SitesService sites;
    @Inject private CommunicationStatusService communicationStatusService;

    @GET @Path("ping") @Produces(MediaType.TEXT_PLAIN)
    public Response ping() { return Response.ok(getSystemStatusUseCase.execute().message()).build(); }

    @GET @Path("version") @Produces(MediaType.APPLICATION_JSON)
    public Response version() { return Response.ok(java.util.Map.of("backendVersion", BuildInfo.VERSION)).build(); }

    @GET @Path("edges") @Produces(MediaType.APPLICATION_JSON)
    public Response edges(@HeaderParam("Authorization") String authorization) {
        Optional<UserAccount> actor = currentUser(authorization);
        if (actor.isEmpty()) return authenticationRequired();
        return Response.ok(edgeHealthRegistry.list().stream()
                .filter(edge -> sites.findByIdentity(edge.clientId(), edge.siteId())
                        .map(site -> access.canAccess(actor.get(), site.id())).orElse(false))
                .toList()).build();
    }

    @GET @Path("communication/{clientId}/{siteId}/{edgeId}") @Produces(MediaType.APPLICATION_JSON)
    public Response communication(@HeaderParam("Authorization") String authorization,
            @PathParam("clientId") String clientId, @PathParam("siteId") String siteId, @PathParam("edgeId") String edgeId,
            @QueryParam("meterId") String meterId, @QueryParam("from") String from, @QueryParam("to") String to) {
        if (authorization != null && !authorization.isBlank()) {
            Optional<UserAccount> actor = currentUser(authorization);
            if (actor.isEmpty()) return authenticationRequired();
            var site = sites.findByIdentity(clientId, siteId).orElse(null);
            if (site == null || !access.canAccess(actor.get(), site.id())) return denied();
        }
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            if (toDate.isBefore(fromDate) || java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) {
                return Response.status(400).entity(new AuthErrorResponse("Invalid date range.")).build();
            }
            return Response.ok(communicationStatusService.findRange(clientId, siteId, edgeId,
                    meterId == null || meterId.isBlank() ? null : meterId, fromDate, toDate)
                    .stream().map(CommunicationStatusView::from).toList()).build();
        } catch (Exception e) {
            return Response.status(400).entity(new AuthErrorResponse("Invalid date range.")).build();
        }
    }

    @DELETE @Path("edges/{clientId}/{siteId}/{edgeId}") @Produces(MediaType.APPLICATION_JSON)
    public Response deleteEdgeHealth(@HeaderParam("Authorization") String authorization,
            @PathParam("clientId") String clientId, @PathParam("siteId") String siteId, @PathParam("edgeId") String edgeId) {
        if (currentAdmin(authorization).isEmpty()) return denied();
        if (edgeHealthRegistry.find(clientId, siteId, edgeId).isEmpty()) return Response.status(404).entity(new AuthErrorResponse("Edge not found.")).build();
        if (!edgeHealthRegistry.isOffline(clientId, siteId, edgeId)) return Response.status(409).entity(new AuthErrorResponse("Only an offline Edge can be removed.")).build();
        try { mqtt.clearRetainedEdgeState(clientId, siteId, edgeId); }
        catch (Exception e) { return Response.status(503).entity(new AuthErrorResponse("Could not clear retained Edge state: " + e.getMessage())).build(); }
        edgeHealthRegistry.remove(clientId, siteId, edgeId);
        edgeConfigurationRegistry.remove(clientId, siteId, edgeId);
        return Response.noContent().build();
    }

    @GET @Path("edges/{clientId}/{siteId}/{edgeId}/config") @Produces(MediaType.APPLICATION_JSON)
    public Response edgeConfig(@HeaderParam("Authorization") String authorization,
            @PathParam("clientId") String clientId, @PathParam("siteId") String siteId, @PathParam("edgeId") String edgeId) {
        if (currentAdmin(authorization).isEmpty()) return denied();
        return edgeConfigurationRegistry.find(clientId, siteId, edgeId)
                .<Response>map(c -> Response.ok(c).build())
                .orElseGet(() -> Response.status(404).entity(new AuthErrorResponse("Edge configuration has not been received yet.")).build());
    }

    @GET @Path("edges/{clientId}/{siteId}/{edgeId}/measurement-data") @Produces(MediaType.APPLICATION_JSON)
    public Response measurementData(@HeaderParam("Authorization") String authorization,
            @PathParam("clientId") String clientId, @PathParam("siteId") String siteId, @PathParam("edgeId") String edgeId) {
        if (currentAdmin(authorization).isEmpty()) return denied();
        return Response.ok(mqtt.rawMeasurementData(clientId, siteId, edgeId)).build();
    }

    @PUT @Path("edges/{clientId}/{siteId}/{edgeId}/config") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response updateEdgeConfig(@HeaderParam("Authorization") String authorization,
            @PathParam("clientId") String clientId, @PathParam("siteId") String siteId, @PathParam("edgeId") String edgeId,
            JsonObject configuration) {
        if (currentAdmin(authorization).isEmpty()) return denied();
        if (!validConfig(configuration)) return Response.status(400).entity(new AuthErrorResponse("Invalid Edge configuration.")).build();
        try {
            mqtt.publishEdgeConfiguration(clientId, siteId, edgeId, configuration);
            return Response.accepted(configuration).build();
        } catch (Exception e) {
            return Response.status(503).entity(new AuthErrorResponse("Could not publish Edge configuration: " + e.getMessage())).build();
        }
    }

    private static boolean validConfig(JsonObject c) {
        if (c == null) return false;
        String clientId = c.getString("clientId", "").trim();
        String siteId = c.getString("siteId", "").trim();
        String edgeId = c.getString("edgeId", "").trim();
        String timeZone = c.getString("timeZone", "").trim();
        if (clientId.isBlank() || siteId.isBlank() || edgeId.isBlank() || timeZone.isBlank()) return false;
        if ("CHANGE_ME".equalsIgnoreCase(clientId) || "CHANGE_ME".equalsIgnoreCase(siteId) || "CHANGE_ME".equalsIgnoreCase(edgeId)) return false;
        if (c.getJsonArray("mqttTargets") == null || c.getJsonArray("mqttTargets").isEmpty()) return false;
        return c.getJsonArray("meters") != null;
    }

    private Optional<UserAccount> currentUser(String authorization) {
        return bearer(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
    }
    private Optional<UserAccount> currentAdmin(String authorization) { return currentUser(authorization).filter(u -> u.role() == UserRole.ADMIN); }
    private Optional<String> bearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        String token = authorization.substring(7).trim(); return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
    private Response authenticationRequired() { return Response.status(401).entity(new AuthErrorResponse("Authentication required.")).build(); }
    private Response denied() { return Response.status(403).entity(new AuthErrorResponse("Administrator role required.")).build(); }
}
