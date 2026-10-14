package pl.atrop.powerguard.bcknd.settings.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.domain.UserRole;
import pl.atrop.powerguard.bcknd.auth.domain.UsersService;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.settings.domain.ApplicationSettingsInput;
import pl.atrop.powerguard.bcknd.settings.domain.SettingsService;

@Path("settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SettingsResource {
    @Inject SettingsService settings;
    @Inject AuthTokenService tokens;
    @Inject UsersService users;

    @GET
    public Response get() {
        return Response.ok(SettingsView.from(settings.get())).build();
    }

    @PUT
    public Response update(@HeaderParam("Authorization") String authorization, SettingsRequest request) {
        if (currentSettingsEditor(authorization).isEmpty()) return denied(authorization);
        if (request == null) return Response.status(Response.Status.BAD_REQUEST)
                .entity(new AuthErrorResponse("Invalid settings data.")).build();
        var updated = settings.update(new ApplicationSettingsInput(
                request.companyName(), request.installationName(), request.address(), request.nip(),
                request.email(), request.phone(), request.contactPerson()));
        return Response.ok(SettingsView.from(updated)).build();
    }

    private Optional<UserAccount> currentUser(String authorization) {
        return bearer(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
    }

    private Optional<UserAccount> currentSettingsEditor(String authorization) {
        return currentUser(authorization)
                .filter(user -> user.role() == UserRole.ADMIN || user.role() == UserRole.INSTALATOR);
    }

    private Optional<String> bearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Response denied(String authorization) {
        return currentUser(authorization).isPresent()
                ? Response.status(Response.Status.FORBIDDEN).entity(new AuthErrorResponse("Administrator or Instalator role required.")).build()
                : Response.status(Response.Status.UNAUTHORIZED).entity(new AuthErrorResponse("Authentication required.")).build();
    }
}
