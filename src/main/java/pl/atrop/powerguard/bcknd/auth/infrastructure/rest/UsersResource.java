package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.domain.*;

@Path("users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {
    @Inject UsersService usersService;
    @Inject AuthTokenService tokens;

    @GET @Path("me")
    public Response me(@HeaderParam("Authorization") String authorization) {
        return currentUser(authorization).<Response>map(user -> Response.ok(UserView.from(user)).build()).orElseGet(this::unauthorized);
    }

    @PUT @Path("me")
    public Response updateMe(@HeaderParam("Authorization") String authorization, SelfEditRequest request) {
        Optional<UserAccount> actor = currentUser(authorization);
        if (actor.isEmpty()) return unauthorized();
        if (request == null) return badRequest("Invalid profile data.");
        return usersService.updateSelf(actor.get().id(), new UserProfileUpdate(request.username(), request.firstname(),
                request.lastname(), request.email(), request.phone(), request.passwordMd5()))
                .<Response>map(user -> Response.ok(UserView.from(user)).build())
                .orElseGet(() -> badRequest("Username is already used or profile data is invalid."));
    }

    @GET
    public Response list(@HeaderParam("Authorization") String authorization) {
        Optional<UserAccount> actor = currentManager(authorization);
        if (actor.isEmpty()) return forbiddenOrUnauthorized(authorization);
        return Response.ok(usersService.findManagedUsers(actor.get().id()).stream().map(UserView::from).toList()).build();
    }

    @POST
    public Response create(@HeaderParam("Authorization") String authorization, ManagedUserRequest request) {
        Optional<UserAccount> actor = currentManager(authorization);
        if (actor.isEmpty()) return forbiddenOrUnauthorized(authorization);
        ManagedUserInput input = toManagedInput(request);
        if (input == null) return badRequest("Invalid user data.");
        return usersService.createManagedUser(actor.get().id(), input)
                .<Response>map(user -> Response.status(Response.Status.CREATED).entity(UserView.from(user)).build())
                .orElseGet(() -> badRequest("User data, role or assigned sites are invalid."));
    }

    @PUT @Path("{id}")
    public Response update(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id, ManagedUserRequest request) {
        Optional<UserAccount> actor = currentManager(authorization);
        if (actor.isEmpty()) return forbiddenOrUnauthorized(authorization);
        ManagedUserInput input = toManagedInput(request);
        if (input == null) return badRequest("Invalid user data.");
        return usersService.updateManagedUser(actor.get().id(), id, input)
                .<Response>map(user -> Response.ok(UserView.from(user)).build())
                .orElseGet(() -> badRequest("User cannot be changed. Check role and assigned sites."));
    }

    @DELETE @Path("{id}")
    public Response delete(@HeaderParam("Authorization") String authorization, @PathParam("id") Long id) {
        Optional<UserAccount> actor = currentManager(authorization);
        if (actor.isEmpty()) return forbiddenOrUnauthorized(authorization);
        return usersService.deleteManagedUser(actor.get().id(), id) ? Response.noContent().build() : badRequest("User cannot be deleted.");
    }

    private Optional<UserAccount> currentUser(String authorization) {
        return bearerToken(authorization).flatMap(tokens::resolve).flatMap(usersService::findById).filter(UserAccount::enabled);
    }
    private Optional<UserAccount> currentManager(String authorization) {
        return currentUser(authorization).filter(u -> u.role() == UserRole.ADMIN || u.role() == UserRole.SERVICE);
    }
    private ManagedUserInput toManagedInput(ManagedUserRequest request) {
        if (request == null) return null;
        try {
            return new ManagedUserInput(request.username(), request.firstname(), request.lastname(), request.email(), request.phone(),
                    request.passwordMd5(), UserRole.from(request.role()), request.enabled(), request.siteIds());
        } catch (IllegalArgumentException ex) { return null; }
    }
    private Optional<String> bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
    private Response unauthorized() { return Response.status(Response.Status.UNAUTHORIZED).entity(new AuthErrorResponse("Authentication required.")).build(); }
    private Response forbiddenOrUnauthorized(String authorization) {
        return currentUser(authorization).isPresent() ? Response.status(Response.Status.FORBIDDEN)
                .entity(new AuthErrorResponse("Administrator or installer role required.")).build() : unauthorized();
    }
    private Response badRequest(String message) { return Response.status(Response.Status.BAD_REQUEST).entity(new AuthErrorResponse(message)).build(); }
}
