package pl.atrop.powerguard.bcknd.auth.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.atrop.powerguard.bcknd.auth.application.LoginCommand;
import pl.atrop.powerguard.bcknd.auth.application.LoginUseCase;
import pl.atrop.powerguard.bcknd.auth.domain.AuthTokenService;

@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject LoginUseCase loginUseCase;
    @Inject AuthTokenService tokens;

    @POST
    @Path("login")
    public Response login(LoginRequest request) {
        LoginCommand command = request == null ? null
                : new LoginCommand(request.username(), request.passwordMd5());

        return loginUseCase.execute(command)
                .<Response>map(user -> Response.ok(new LoginResponse(
                        true, user.id(), user.username(), user.firstname(), user.lastname(), user.email(), user.phone(),
                        user.role().name(), user.siteIds(), user.token())).build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new AuthErrorResponse("Invalid username or password.")).build());
    }

    @POST
    @Path("logout")
    public Response logout(@HeaderParam("Authorization") String authorization) {
        bearerToken(authorization).ifPresent(tokens::revoke);
        return Response.noContent().build();
    }

    private java.util.Optional<String> bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return java.util.Optional.empty();
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }
}
