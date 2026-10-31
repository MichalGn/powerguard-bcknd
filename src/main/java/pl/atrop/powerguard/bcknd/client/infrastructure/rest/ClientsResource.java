package pl.atrop.powerguard.bcknd.client.infrastructure.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Optional;
import pl.atrop.powerguard.bcknd.auth.application.SiteAccessGuard;
import pl.atrop.powerguard.bcknd.auth.domain.UserAccount;
import pl.atrop.powerguard.bcknd.auth.infrastructure.rest.AuthErrorResponse;
import pl.atrop.powerguard.bcknd.client.domain.*;
import pl.atrop.powerguard.bcknd.site.domain.SitesService;

@Path("clients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientsResource {
    @Inject ClientsService service;
    @Inject SitesService sites;
    @Inject SiteAccessGuard access;

    @GET
    public Response publicClients(@HeaderParam("Authorization") String authorization) {
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty()) return Response.status(Response.Status.UNAUTHORIZED).entity(new AuthErrorResponse("Authentication required.")).build();
        var visible = service.findPublicClients().stream().filter(client ->
                sites.findAll().stream().anyMatch(site -> site.clientPk().equals(client.id()) && access.canAccess(actor.get(), site.id()))).map(ClientView::from).toList();
        return Response.ok(visible).build();
    }
    @GET @Path("admin")
    public Response all(@HeaderParam("Authorization") String authorization) {
        Optional<UserAccount> actor = access.currentUser(authorization);
        if (actor.isEmpty() || !access.isAdminOrService(actor.get())) return denied();
        return Response.ok(service.findAll().stream().map(ClientView::from).toList()).build();
    }
    @POST
    public Response create(@HeaderParam("Authorization") String authorization, ClientRequest r){
        Optional<UserAccount> actor=access.currentUser(authorization); if(actor.isEmpty()||!access.isAdmin(actor.get())) return denied();
        if(r==null) return bad("Invalid client data.");
        return service.create(new ClientConfigurationInput(r.clientId(),r.name(),r.enabled()))
                .<Response>map(c->Response.status(201).entity(ClientView.from(c)).build()).orElseGet(()->bad("Invalid or duplicate Client ID."));
    }
    @PUT @Path("{clientId}")
    public Response update(@HeaderParam("Authorization") String authorization,@PathParam("clientId") String clientId,ClientRequest r){
        Optional<UserAccount> actor=access.currentUser(authorization); if(actor.isEmpty()||!access.isAdmin(actor.get())) return denied();
        if(r==null) return bad("Invalid client data.");
        return service.update(clientId,new ClientConfigurationInput(clientId,r.name(),r.enabled()))
                .<Response>map(c->Response.ok(ClientView.from(c)).build()).orElseGet(()->bad("Invalid client data."));
    }
    @DELETE @Path("{clientId}")
    public Response delete(@HeaderParam("Authorization") String authorization,@PathParam("clientId") String clientId){
        Optional<UserAccount> actor=access.currentUser(authorization); if(actor.isEmpty()||!access.isAdmin(actor.get())) return denied();
        if(!service.delete(clientId)) return Response.status(409).entity(new AuthErrorResponse("Client cannot be deleted while sites use it.")).build();
        return Response.noContent().build();
    }
    private Response denied(){return Response.status(403).entity(new AuthErrorResponse("Administrator role required.")).build();}
    private Response bad(String m){return Response.status(400).entity(new AuthErrorResponse(m)).build();}
}
