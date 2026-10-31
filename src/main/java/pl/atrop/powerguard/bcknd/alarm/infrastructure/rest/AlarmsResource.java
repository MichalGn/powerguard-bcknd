package pl.atrop.powerguard.bcknd.alarm.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.LocalDate;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.alarm.domain.AlarmsService;
import pl.atrop.powerguard.bcknd.auth.application.SiteAccessGuard;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.meter.domain.MeterConfiguration;
import pl.atrop.powerguard.bcknd.meter.domain.MetersService;

@Path("alarms")
@Produces(MediaType.APPLICATION_JSON)
public class AlarmsResource {
    @Inject AlarmsService service;
    @Inject MetersService meters;
    @Inject SiteAccessGuard access;

    @GET @Path("active/{meterId}")
    public Response active(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        return Response.ok(service.active(meterId).stream().map(AlarmView::from).toList()).build();
    }

    @GET @Path("day/{meterId}")
    public Response day(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId, @QueryParam("date") String date) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        try { return Response.ok(service.findDay(meterId, LocalDate.parse(date)).stream().map(AlarmView::from).toList()).build(); }
        catch (Exception e) { return Response.status(Response.Status.BAD_REQUEST).build(); }
    }

    @GET @Path("range/{meterId}")
    public Response range(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId,
            @QueryParam("from") String from, @QueryParam("to") String to) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            if (toDate.isBefore(fromDate) || java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok(service.findRange(meterId, fromDate, toDate).stream().map(AlarmView::from).toList()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @PUT @Path("{alarmId}/acknowledge") @Consumes(MediaType.TEXT_PLAIN)
    public Response acknowledge(@HeaderParam("Authorization") String authorization, @PathParam("alarmId") Long alarmId, String acknowledgedBy) {
        var alarm = service.findById(alarmId).orElse(null);
        if (alarm == null) return Response.status(Response.Status.NOT_FOUND).build();
        MeterConfiguration meter = meters.findById(alarm.meterId()).orElse(null);
        if (meter == null) return Response.status(Response.Status.NOT_FOUND).build();

        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new AuthErrorResponse("Authentication required.")).build();
        if (!access.canAccess(actor.get(), meter.sitePk())) return forbidden();

        String by = (acknowledgedBy == null || acknowledgedBy.isBlank()) ? "dashboard" : acknowledgedBy;
        return service.acknowledge(alarmId, by).map(a -> Response.ok(AlarmView.from(a)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    private boolean allowedIfAuthenticated(String authorization, Integer meterId) {
        if (authorization == null || authorization.isBlank()) return false;
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return false;
        MeterConfiguration meter = meters.findById(meterId).orElse(null);
        return meter != null && access.canAccess(actor.get(), meter.sitePk());
    }
    private Response forbidden() { return Response.status(Response.Status.FORBIDDEN).entity(new AuthErrorResponse("No access to this site.")).build(); }
}
