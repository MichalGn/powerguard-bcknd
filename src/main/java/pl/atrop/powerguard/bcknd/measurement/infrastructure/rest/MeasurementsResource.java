package pl.atrop.powerguard.bcknd.measurement.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.atrop.powerguard.bcknd.auth.application.SiteAccessGuard;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.measurement.domain.*;
import pl.atrop.powerguard.bcknd.meter.domain.MeterConfiguration;
import pl.atrop.powerguard.bcknd.meter.domain.MetersService;

@Path("measurements")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MeasurementsResource {
    private static final Logger LOG = Logger.getLogger(MeasurementsResource.class.getName());

    @Inject MeasurementsService service;
    @Inject MetersService meters;
    @Inject SiteAccessGuard access;
    @PersistenceContext(unitName = "powerguradPU") EntityManager em;

    @GET @Path("latest/{meterId}")
    public Response latest(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        return service.latest(meterId).<Response>map(m -> Response.ok(MeasurementSnapshotView.from(m)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET @Path("day/{meterId}")
    public Response day(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId, @QueryParam("date") String date) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        try {
            LocalDate parsed = LocalDate.parse(date);
            return Response.ok(service.findDay(meterId, parsed).stream().map(MeasurementView::from).toList()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new AuthErrorResponse("Invalid date.")).build();
        }
    }

    @GET @Path("range/{meterId}")
    public Response range(@HeaderParam("Authorization") String authorization, @PathParam("meterId") Integer meterId,
            @QueryParam("from") String from, @QueryParam("to") String to) {
        if (!allowedIfAuthenticated(authorization, meterId)) return forbidden();
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            if (toDate.isBefore(fromDate) || java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) > 31) {
                return Response.status(Response.Status.BAD_REQUEST).entity(new AuthErrorResponse("Invalid date range. Maximum 32 days.")).build();
            }
            long startedAt = System.currentTimeMillis();
            List<MeasurementReading> readings = service.findRange(meterId, fromDate, toDate);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            LOG.info(() -> String.format(
                    "[MEASUREMENTS-RANGE] meterId=%d from=%s to=%s count=%d elapsedMs=%d",
                    meterId, fromDate, toDate, readings.size(), elapsedMs));
            return Response.ok(readings.stream().map(MeasurementView::from).toList()).build();
        } catch (Exception e) {
            LOG.log(Level.WARNING, String.format(
                    "[MEASUREMENTS-RANGE] failed meterId=%s from=%s to=%s",
                    meterId, from, to), e);
            return Response.status(Response.Status.BAD_REQUEST).entity(new AuthErrorResponse("Invalid date range.")).build();
        }
    }

    @POST
    public Response record(@HeaderParam("Authorization") String authorization, MeasurementRequest request) {
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return Response.status(Response.Status.UNAUTHORIZED).entity(new AuthErrorResponse("Authentication required.")).build();
        if (!access.isManager(actor.get())) return forbidden();
        if (request == null) return bad();
        MeterConfiguration meter = meters.findById(request.meterId()).orElse(null);
        if (meter == null || !access.canAccess(actor.get(), meter.sitePk())) return forbidden();
        MeasurementInput input = new MeasurementInput(request.meterId(), request.measuredAt(), null, request.activePowerKw(), null, null,
                request.activeEnergyImportKwh(), request.activeEnergyExportKwh(), request.currentQuarterPowerKw(),
                request.voltageL1V(), request.voltageL2V(), request.voltageL3V(), request.currentL1A(), request.currentL2A(),
                request.currentL3A(), request.frequencyHz(), null, null);
        return service.record(input).<Response>map(m -> Response.status(Response.Status.CREATED).entity(MeasurementView.from(m)).build())
                .orElseGet(this::bad);
    }

    /** Admin-only, explicit date-scoped removal of raw measurement history for one meter. */
    @DELETE @Path("history/{meterId}")
    @Transactional
    public Response deleteHistory(@HeaderParam("Authorization") String authorization,
            @PathParam("meterId") Integer meterId,
            @QueryParam("from") String from, @QueryParam("to") String to,
            @QueryParam("confirm") String confirmation) {
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return Response.status(401).build();
        if (!access.isAdmin(actor.get())) return forbidden();
        if (meterId == null || meters.findById(meterId).isEmpty()) return Response.status(404).build();
        if (!"DELETE".equals(confirmation)) return Response.status(400)
                .entity(new AuthErrorResponse("Explicit DELETE confirmation required.")).build();
        try {
            LocalDate a = LocalDate.parse(from), z = LocalDate.parse(to);
            // Deletion can cover any historical span, including readings from today (UTC).
            // Future end dates are rejected. Only readings up to the current instant are removed.
            Instant now = Instant.now();
            if (z.isBefore(a) || z.isAfter(LocalDate.ofInstant(now, ZoneOffset.UTC))) {
                return Response.status(400).entity(new AuthErrorResponse("Invalid date range: select dates through today (UTC).")).build();
            }
            Instant start = a.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endExclusive = z.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = endExclusive.isAfter(now) ? now : endExclusive;
            int removed = em.createQuery("DELETE FROM Measurements m WHERE m.meterId = :meterId AND m.measuredAt >= :start AND m.measuredAt < :end")
                    .setParameter("meterId", meterId).setParameter("start", start).setParameter("end", end).executeUpdate();
            LOG.warning("[ADMIN-HISTORY-DELETE] actor=" + actor.get().id() + " meter=" + meterId + " from=" + a + " to=" + z + " removed=" + removed);
            return Response.ok(java.util.Map.of("deleted", removed)).build();
        } catch (Exception ex) {
            return Response.status(400).entity(new AuthErrorResponse("Invalid date range.")).build();
        }
    }

    private boolean allowedIfAuthenticated(String authorization, Integer meterId) {
        if (authorization == null || authorization.isBlank()) return false;
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return false;
        MeterConfiguration meter = meters.findById(meterId).orElse(null);
        return meter != null && access.canAccess(actor.get(), meter.sitePk());
    }
    private Response forbidden() { return Response.status(Response.Status.FORBIDDEN).entity(new AuthErrorResponse("No access to this site.")).build(); }
    private Response bad() { return Response.status(Response.Status.BAD_REQUEST).entity(new AuthErrorResponse("Invalid measurement data.")).build(); }
}
