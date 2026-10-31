package pl.atrop.powerguard.bcknd.meter.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import pl.atrop.powerguard.bcknd.auth.application.SiteAccessGuard;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.meter.domain.*;
import pl.atrop.powerguard.bcknd.site.domain.SitesService;

@Path("meters")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MetersResource {

    @Inject
    MetersService service;
    @Inject
    SiteAccessGuard access;
    @Inject
    SitesService sites;

    @GET
    public Response publicMeters(@HeaderParam("Authorization") String a) {
        Optional<UserAccount> actor = access.currentUser(a);
        if (actor.isEmpty()) {
            return denied();
        }
        return Response.ok(service.findPublicMeters().stream().filter(m -> access.canAccess(actor.get(), m.sitePk())).map(MeterView::from).toList()).build();
    }

    @GET
    @Path("admin")
    public Response all(@HeaderParam("Authorization") String a) {
        Optional<UserAccount> actor = access.currentUser(a);
        if (actor.isEmpty()) {
            return denied();
        }
        if (actor.get().role() == pl.atrop.powerguard.bcknd.auth.domain.UserRole.USER) {
            return forbidden();
        }
        return Response.ok(service.findAll().stream().filter(m -> access.canAccess(actor.get(), m.sitePk())).map(MeterView::from).toList()).build();
    }

    @POST
    public Response create(@HeaderParam("Authorization") String a, MeterRequest r) {
        Optional<UserAccount> actor = access.currentUser(a);
        if (actor.isEmpty()) {
            return denied();
        }
        if (!access.isManager(actor.get())) {
            return forbidden();
        }
        MeterConfigurationInput i = input(r);
        var site = i == null ? Optional.<pl.atrop.powerguard.bcknd.site.domain.SiteConfiguration>empty() : sites.findByIdentity(i.clientId(), i.siteId());
        if (i == null || site.isEmpty() || !access.canAccess(actor.get(), site.get().id())) {
            return forbidden();
        }
        return service.create(i).<Response>map(m -> Response.status(201).entity(MeterView.from(m)).build()).orElseGet(this::bad);
    }

    @PUT
    @Path("{id}")
    public Response update(@HeaderParam("Authorization") String a, @PathParam("id") Integer id, MeterRequest r) {
        Optional<UserAccount> actor = access.currentUser(a);
        if (actor.isEmpty()) {
            return denied();
        }
        if (!access.isManager(actor.get()) && actor.get().role() != pl.atrop.powerguard.bcknd.auth.domain.UserRole.ADVANCED_USER) {
            return forbidden();
        }
        MeterConfiguration old = service.findById(id).orElse(null);
        MeterConfigurationInput i = input(r);
        var target = i == null ? Optional.<pl.atrop.powerguard.bcknd.site.domain.SiteConfiguration>empty() : sites.findByIdentity(i.clientId(), i.siteId());
        if (old == null || i == null || target.isEmpty() || !access.canAccess(actor.get(), old.sitePk()) || !access.canAccess(actor.get(), target.get().id())) {
            return forbidden();
        }
        if (actor.get().role() == pl.atrop.powerguard.bcknd.auth.domain.UserRole.ADVANCED_USER) {
            if (!java.util.Objects.equals(old.clientId(), i.clientId()) || !java.util.Objects.equals(old.siteId(), i.siteId())
                    || !java.util.Objects.equals(old.edgeId(), i.edgeId()) || !java.util.Objects.equals(old.uniqueKey(), i.uniqueKey())
                    || !java.util.Objects.equals(old.name(), i.name()) || old.enabled() != i.enabled()) {
                return forbidden();
            }
            // Use authoritative persisted values for fields that an advanced user cannot edit.
            i = new MeterConfigurationInput(old.clientId(), old.siteId(), old.edgeId(), old.uniqueKey(), old.name(),
                    i.instantaneousAlarmOnKw(), i.instantaneousAlarmOffKw(), i.quarterHourAlarmOnKw(), i.quarterHourAlarmOffKw(), old.enabled());
        }
        return service.update(id, i).<Response>map(m -> Response.ok(MeterView.from(m)).build()).orElseGet(this::bad);
    }

    @PUT
    @Path("{id}/alarm-thresholds")
    public Response updateAlarmThresholds(@HeaderParam("Authorization") String a, @PathParam("id") Integer id, AlarmThresholdsRequest r) {
        var actor = access.currentUser(a);
        if (actor.isEmpty()) {
            return denied();
        }
        if (actor.get().role() == pl.atrop.powerguard.bcknd.auth.domain.UserRole.USER) {
            return forbidden();
        }
        var old = service.findById(id).orElse(null);
        if (old == null || !access.canAccess(actor.get(), old.sitePk())) {
            return forbidden();
        }
        if (r == null) {
            return bad();
        }
        var input = new MeterConfigurationInput(old.clientId(), old.siteId(), old.edgeId(), old.uniqueKey(), old.name(),
                r.instantaneousAlarmOnKw(), r.instantaneousAlarmOffKw(), r.quarterHourAlarmOnKw(), r.quarterHourAlarmOffKw(), old.enabled());
        return service.update(id, input).<Response>map(m -> Response.ok(MeterView.from(m)).build()).orElseGet(this::bad);
    }

    public record AlarmThresholdsRequest(java.math.BigDecimal instantaneousAlarmOnKw, java.math.BigDecimal instantaneousAlarmOffKw, java.math.BigDecimal quarterHourAlarmOnKw, java.math.BigDecimal quarterHourAlarmOffKw) {}

    @PUT
    @Path("{id}/move-up")
    public Response up(@HeaderParam("Authorization") String a, @PathParam("id") Integer id) {
        var actor = access.currentUser(a);
        var m = service.findById(id).orElse(null);
        if (actor.isEmpty()) {
            return denied();
        }
        if (!access.isManager(actor.get())) {
            return forbidden();
        }
        if (m == null || !access.canAccess(actor.get(), m.sitePk())) {
            return forbidden();
        }
        return service.moveUp(id) ? Response.noContent().build() : Response.status(409).entity(new AuthErrorResponse("Meter cannot be moved up.")).build();
    }

    @PUT
    @Path("{id}/move-down")
    public Response down(@HeaderParam("Authorization") String a, @PathParam("id") Integer id) {
        var actor = access.currentUser(a);
        var m = service.findById(id).orElse(null);
        if (actor.isEmpty()) {
            return denied();
        }
        if (!access.isManager(actor.get())) {
            return forbidden();
        }
        if (m == null || !access.canAccess(actor.get(), m.sitePk())) {
            return forbidden();
        }
        return service.moveDown(id) ? Response.noContent().build() : Response.status(409).entity(new AuthErrorResponse("Meter cannot be moved down.")).build();
    }

    @DELETE
    @Path("{id}")
    public Response del(@HeaderParam("Authorization") String a, @PathParam("id") Integer id) {
        var actor = access.currentUser(a);
        var m = service.findById(id).orElse(null);
        if (actor.isEmpty()) {
            return denied();
        }
        if (!access.isManager(actor.get())) {
            return forbidden();
        }
        if (m == null || !access.canAccess(actor.get(), m.sitePk())) {
            return forbidden();
        }
        return service.delete(id) ? Response.noContent().build() : bad();
    }

    private MeterConfigurationInput input(MeterRequest r) {
        return r == null ? null : new MeterConfigurationInput(r.clientId(), r.siteId(), r.edgeId(), r.uniqueKey(), r.name(), r.instantaneousAlarmOnKw(), r.instantaneousAlarmOffKw(), r.quarterHourAlarmOnKw(), r.quarterHourAlarmOffKw(), r.enabled());
    }

    private Response denied() {
        return Response.status(401).entity(new AuthErrorResponse("Authentication required.")).build();
    }

    private Response forbidden() {
        return Response.status(403).entity(new AuthErrorResponse("No access to the selected site.")).build();
    }

    private Response bad() {
        return Response.status(400).entity(new AuthErrorResponse("Invalid meter data, hysteresis thresholds, or Meter ID already used for the selected edge.")).build();
    }
}
