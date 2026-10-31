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
import pl.atrop.powerguard.bcknd.auth.domain.*;
import pl.atrop.powerguard.bcknd.auth.infrastructure.security.TrustedDeviceService;

@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject LoginUseCase loginUseCase;
    @Inject AuthTokenService tokens;
    @Inject UsersService users;
    @Inject TrustedDeviceService devices;

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

    @Inject pl.atrop.powerguard.bcknd.auth.infrastructure.security.InstallationAutoLoginService autoLogin;

    /** Public status intentionally exposes only whether guest access is enabled. */
    @jakarta.ws.rs.GET @Path("installation-auto-login")
    public Response installationAutoLoginStatus() {
        return Response.ok(java.util.Map.of("enabled", autoLogin.configuredUserId().isPresent())).build();
    }

    /** Shared, passwordless default session: requires explicit administrator configuration. */
    @POST @Path("installation-auto-login/exchange")
    public Response installationAutoLoginExchange() {
        var account = autoLogin.configuredUserId().flatMap(users::findById)
                .filter(UserAccount::enabled);
        if (account.isEmpty()) return Response.status(401).entity(new AuthErrorResponse("Automatic login unavailable.")).build();
        UserAccount u = account.get();
        return Response.ok(new LoginResponse(true, u.id(), u.username(), u.firstname(), u.lastname(),
                u.email(), u.phone(), u.role().name(), u.siteIds(), tokens.issue(u.id()))).build();
    }

    @jakarta.ws.rs.GET @Path("installation-auto-login/configuration")
    public Response readInstallationAutoLogin(@HeaderParam("Authorization") String authorization) {
        var actor = bearerToken(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
        if (actor.isEmpty()) return Response.status(401).build();
        if (actor.get().role() != UserRole.ADMIN && actor.get().role() != UserRole.SERVICE) return Response.status(403).build();
        return Response.ok(java.util.Map.of("userId", autoLogin.configuredUserId().orElse(0L))).build();
    }

    public record InstallationAutoLoginRequest(Long userId) { }

    @jakarta.ws.rs.PUT @Path("installation-auto-login")
    public Response configureInstallationAutoLogin(@HeaderParam("Authorization") String authorization,
                                                   InstallationAutoLoginRequest request) {
        var actor = bearerToken(authorization).flatMap(tokens::resolve).flatMap(users::findById)
                .filter(UserAccount::enabled);
        if (actor.isEmpty()) return Response.status(401).build();
        if (actor.get().role() != UserRole.ADMIN && actor.get().role() != UserRole.SERVICE) return Response.status(403).build();
        if (request == null) return Response.status(400).build();
        if (request.userId() != null) {
            var target = users.findById(request.userId());
            if (target.isEmpty() || !target.get().enabled())
                return Response.status(400).entity(new AuthErrorResponse("Select an enabled account.")).build();
        }
        autoLogin.configure(request.userId());
        return Response.ok(java.util.Map.of("enabled", request.userId() != null,
                "userId", request.userId() == null ? 0 : request.userId())).build();
    }

    @POST
    @Path("logout")
    public Response logout(@HeaderParam("Authorization") String authorization) {
        bearerToken(authorization).ifPresent(tokens::revoke);
        return Response.noContent().build();
    }

    /** Authenticated users may enable automatic login only for their own account. */
    public record DeviceProvisionRequest(Long userId) { }

    @POST @Path("trusted-device/provision")
    public Response provisionDevice(@HeaderParam("Authorization") String authorization, DeviceProvisionRequest request) {
        java.util.Optional<UserAccount> actor = bearerToken(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
        if (actor.isEmpty()) return Response.status(401).entity(new AuthErrorResponse("Authentication required.")).build();
        if (request == null || request.userId() == null || !request.userId().equals(actor.get().id())) return Response.status(403).entity(new AuthErrorResponse("Only your own account may be provisioned.")).build();
        java.util.Optional<UserAccount> target = users.findById(request.userId());
        if (target.isEmpty() || !target.get().enabled()) {
            return Response.status(400).entity(new AuthErrorResponse("Active account required.")).build();
        }
        return Response.ok(java.util.Map.of("deviceToken", devices.register(target.get()))).build();
    }

    public record DeviceRequest(String deviceToken) { }

    @POST @Path("trusted-device/exchange")
    public Response exchangeDevice(DeviceRequest request) {
        java.util.Optional<UserAccount> user = devices.exchange(request == null ? null : request.deviceToken()).flatMap(users::findById);
        if (user.isEmpty()) return Response.status(401).entity(new AuthErrorResponse("Device authorization expired or revoked.")).build();
        UserAccount u = user.get();
        return Response.ok(new LoginResponse(true, u.id(), u.username(), u.firstname(), u.lastname(), u.email(), u.phone(), u.role().name(), u.siteIds(), tokens.issue(u.id()))).build();
    }

    @POST @Path("trusted-device/revoke")
    public Response revokeDevice(@HeaderParam("Authorization") String authorization, DeviceRequest request) {
        java.util.Optional<UserAccount> actor = bearerToken(authorization).flatMap(tokens::resolve).flatMap(users::findById).filter(UserAccount::enabled);
        if (actor.isEmpty()) return Response.status(401).build();
        String secret = request == null ? null : request.deviceToken();
        java.util.Optional<Long> owner = devices.exchange(secret);
        if (owner.isPresent() && (owner.get().equals(actor.get().id()) || actor.get().role() == UserRole.ADMIN)) devices.revoke(secret);
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
