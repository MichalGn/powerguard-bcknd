package pl.atrop.powerguard.bcknd.measurement.infrastructure.mqtt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.*;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.paho.client.mqttv3.*;
import pl.atrop.powerguard.bcknd.measurement.domain.*;
import pl.atrop.powerguard.bcknd.edge.domain.EdgesService;
import pl.atrop.powerguard.bcknd.measurement.infrastructure.websocket.LiveMeasurementsSocket;
import pl.atrop.powerguard.bcknd.meter.domain.MeterConfiguration;
import pl.atrop.powerguard.bcknd.meter.domain.MetersRepository;
import pl.atrop.powerguard.bcknd.site.domain.SitesService;
import pl.atrop.powerguard.bcknd.system.application.*;
import pl.atrop.powerguard.bcknd.system.domain.*;
import pl.atrop.powerguard.bcknd.shared.infrastructure.BuildInfo;

/**
 * MQTT ingestion point.
 * Preferred hierarchy:
 * powerguard/{clientId}/{siteId}/{edgeId}/{meterId}/telemetry
 * powerguard/{clientId}/{siteId}/{edgeId}/{meterId}/status
 * powerguard/{clientId}/{siteId}/{edgeId}/health
 * powerguard/{clientId}/{siteId}/{edgeId}/config/state
 *
 * Legacy topics without clientId remain accepted while old Edge versions are deployed.
 */
@ApplicationScoped
public class PowerguardMqttSubscriber implements MqttCallbackExtended {
    private static final String[] TOPICS = {
            "powerguard/+/+/+/telemetry", "powerguard/+/+/+/status", "powerguard/+/+/health", "powerguard/+/+/config/state",
            "powerguard/+/+/+/+/telemetry", "powerguard/+/+/+/+/status", "powerguard/+/+/+/health", "powerguard/+/+/+/config/state"
    };

    @Inject MeasurementsService measurements;
    @Inject MetersRepository meters;
    @Inject SitesService sites;
    @Inject EdgesService edges;
    @Inject EdgeHealthRegistry edgeHealthRegistry;
    @Inject EdgeConfigurationRegistry edgeConfigurationRegistry;
    @Inject CommunicationStatusService communicationStatusService;

    private MqttAsyncClient client;
    private final Map<String, JsonObject> latestRawMeasurements = new ConcurrentHashMap<>();

    public List<JsonObject> rawMeasurementData(String clientId, String siteId, String edgeId) {
        return latestRawMeasurements.values().stream()
                .filter(v -> Objects.equals(clientId, v.getString("clientId", null))
                        && Objects.equals(siteId, v.getString("siteId", null))
                        && Objects.equals(edgeId, v.getString("edgeId", null)))
                .sorted(Comparator.comparing(v -> v.getString("meterUniqueKey", "")))
                .toList();
    }

    private static String rawKey(String clientId, String siteId, String edgeId, String meterId) {
        return clientId + "\u001f" + siteId + "\u001f" + edgeId + "\u001f" + meterId;
    }

    private void clearRawMeasurementData(String clientId, String siteId, String edgeId) {
        latestRawMeasurements.entrySet().removeIf(e -> {
            JsonObject v = e.getValue();
            return Objects.equals(clientId, v.getString("clientId", null))
                    && Objects.equals(siteId, v.getString("siteId", null))
                    && Objects.equals(edgeId, v.getString("edgeId", null));
        });
    }

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object event) { start(); }
    public void onShutdown(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) { stop(); }

    private void start() {
        String broker = setting("MQTT_BROKER","10.78.0.251");// "127.0.0.1");
        int port = intSetting("MQTT_PORT", 1884);//1883);
        String username = setting("MQTT_USERNAME", "powerGuard");
        String password = setting("MQTT_PASSWORD", "guardPower");
        String uri = broker.contains("://") ? broker : "tcp://" + broker + ":" + port;
        try {
            client = new MqttAsyncClient(uri, "powerguard-backend-" + UUID.randomUUID());
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true); options.setCleanSession(true); options.setConnectionTimeout(10);
            if (!username.isBlank()) options.setUserName(username);
            if (!password.isBlank()) options.setPassword(password.toCharArray());
            System.out.println("[VERSION] PowerGuard Backend " + BuildInfo.VERSION);
            System.out.println("[MQTT] Starting backend subscriber");
            System.out.println("[MQTT] Broker=" + uri + ", user=" + username);
            client.connect(options).waitForCompletion();
            subscribe();
            System.out.println("[MQTT] Backend connected and subscribed");
        } catch (Exception e) { System.err.println("[MQTT] Backend subscriber startup failed: " + e.getMessage()); }
    }

    private void stop() {
        try { if (client != null && client.isConnected()) client.disconnect().waitForCompletion(3000); if (client != null) client.close(); }
        catch (Exception ignored) { }
    }

    @Override public void connectComplete(boolean reconnect, String serverURI) {
        if (!reconnect) return;
        try { subscribe(); } catch (MqttException e) { System.err.println("[MQTT] Re-subscribe failed: " + e.getMessage()); }
    }

    private void subscribe() throws MqttException {
        int[] qos = new int[TOPICS.length]; Arrays.fill(qos, 1);
        client.subscribe(TOPICS, qos).waitForCompletion();
        System.out.println("[MQTT] Subscribed to legacy + client-aware PowerGuard topics");
    }

    /** Client-aware Edge configuration command. */
    public void publishEdgeConfiguration(String clientId, String siteId, String edgeId, JsonObject configuration) throws MqttException {
        if (client == null || !client.isConnected()) throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        String topic = "powerguard/" + clientId + "/" + siteId + "/" + edgeId + "/config/set";
        MqttMessage message = new MqttMessage(configuration.toString().getBytes(StandardCharsets.UTF_8));
        message.setQos(1); message.setRetained(false); client.publish(topic, message).waitForCompletion();
        System.out.println("[MQTT] Edge configuration command published to " + topic + " (clientId=" + clientId + ")");
    }

    public void clearRetainedEdgeState(String clientId, String siteId, String edgeId) throws MqttException {
        if (client == null || !client.isConnected()) throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        // Clear both namespaces during transition.
        clearRetainedTopic("powerguard/" + siteId + "/" + edgeId + "/health");
        clearRetainedTopic("powerguard/" + siteId + "/" + edgeId + "/config/state");
        clearRetainedTopic("powerguard/" + clientId + "/" + siteId + "/" + edgeId + "/health");
        clearRetainedTopic("powerguard/" + clientId + "/" + siteId + "/" + edgeId + "/config/state");
    }

    private void clearRetainedTopic(String topic) throws MqttException {
        MqttMessage tombstone = new MqttMessage(new byte[0]); tombstone.setQos(1); tombstone.setRetained(true); client.publish(topic, tombstone).waitForCompletion();
    }

    @Override public void connectionLost(Throwable cause) { System.err.println("[MQTT] Connection lost: " + (cause == null ? "unknown" : cause.getMessage())); }
    @Override public void deliveryComplete(IMqttDeliveryToken token) { }

    @Override public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        TopicIdentity ti = TopicIdentity.from(topic);
        if (payload.isBlank()) {
            if (ti != null) {
                String clientId = ti.clientId;
                if (blank(clientId) && !blank(ti.siteId)) clientId = sites.findUniqueBySiteId(ti.siteId).map(s -> s.clientId()).orElse(null);
                if (!blank(clientId)) {
                    if (topic.endsWith("/health")) edgeHealthRegistry.remove(clientId, ti.siteId, ti.edgeId);
                    if (topic.endsWith("/config/state")) edgeConfigurationRegistry.remove(clientId, ti.siteId, ti.edgeId);
                }
            }
            return;
        }
        try {
            JsonObject json = Json.createReader(new StringReader(payload)).readObject();
            String type = string(json, "messageType");
            if (topic.endsWith("/telemetry") || "meter.telemetry".equals(type)) handleTelemetry(topic, json, ti);
            else if (topic.endsWith("/health")) handleHealth(topic, json, ti);
            else if (topic.endsWith("/config/state")) handleConfigState(topic, json, ti);
            else if (topic.endsWith("/status") || "meter.status".equals(type)) handleStatus(topic, json, ti);
        } catch (Exception e) { System.err.println("[MQTT] Invalid message on " + topic + ": " + e.getMessage()); }
    }

    private void handleTelemetry(String topic, JsonObject root, TopicIdentity ti) {
        JsonObject meterJson = object(root, "meter"), timestamps = object(root, "timestamps"), values = object(root, "measurements");
        JsonObject voltage = object(values, "voltageV"), current = object(values, "currentA"), quality = object(root, "quality"), edgeJson = object(root, "edge");
        String clientId = firstText(string(edgeJson, "clientId"), string(root, "clientId"), ti == null ? null : ti.clientId);
        String siteId = firstText(string(edgeJson, "siteId"), ti == null ? null : ti.siteId);
        String edgeId = firstText(string(edgeJson, "edgeId"), ti == null ? null : ti.edgeId);
        String meterKey = firstText(string(meterJson, "meterId"), ti == null ? null : ti.meterId);
        if (blank(siteId) || blank(edgeId) || blank(meterKey)) return;

        Optional<MeterConfiguration> meter = blank(clientId)
                ? meters.findByLegacyIdentity(siteId, edgeId, meterKey)
                : meters.findByIdentity(clientId, siteId, edgeId, meterKey);
        if (meter.isEmpty()) {
            System.err.println("[MQTT] Unknown meter clientId='" + clientId + "', siteId='" + siteId + "', edgeId='" + edgeId + "', meterId='" + meterKey + "'.");
            return;
        }
        clientId = meter.get().clientId(); siteId = meter.get().siteId(); edgeId = meter.get().edgeId();
        Instant edgeAt = instant(timestamps, "edgeTimestamp", Instant.now());

        JsonValue rawObis = root.get("rawObis");
        if (rawObis != null && rawObis.getValueType() != JsonValue.ValueType.NULL) {
            JsonObjectBuilder rawSnapshot = Json.createObjectBuilder()
                    .add("clientId", clientId)
                    .add("siteId", siteId)
                    .add("edgeId", edgeId)
                    .add("meterUniqueKey", meterKey)
                    .add("receivedAt", Instant.now().toString())
                    .add("measuredAt", edgeAt.toString())
                    .add("meterInfo", meterJson)
                    .add("quality", quality)
                    .add("rawObis", rawObis);
            Instant meterAt = instant(timestamps, "meterTimestamp", null);
            if (meterAt == null) rawSnapshot.addNull("meterTimestamp");
            else rawSnapshot.add("meterTimestamp", meterAt.toString());
            latestRawMeasurements.put(rawKey(clientId, siteId, edgeId, meterKey), rawSnapshot.build());
        }
        MeasurementInput input = new MeasurementInput(meter.get().id(), edgeAt, instant(timestamps,"meterTimestamp",null),
                number(values,"activePowerKw"), number(values,"activeEnergyImportKwh"), number(values,"activeEnergyExportKwh"), firstNumber(values,"currentQuarterPowerKw","current15MinPowerKw"),
                number(voltage,"l1"),number(voltage,"l2"),number(voltage,"l3"),number(current,"l1"),number(current,"l2"),number(current,"l3"),number(values,"frequencyHz"),longNumber(quality,"readDurationMs"),bool(quality,"bccValid"));
        if (measurements.record(input).isEmpty()) return;
        JsonObject live = Json.createObjectBuilder().add("messageType","meter.live").add("meterId",meter.get().id())
                .add("clientId",clientId).add("siteId",siteId).add("edgeId",edgeId).add("meterUniqueKey",meterKey).add("status","ONLINE").add("measuredAt",edgeAt.toString())
                .add("currentPowerKw",jsonNumberOrNull(input.activePowerKw())).add("currentQuarterPowerKw",jsonNumberOrNull(input.currentQuarterPowerKw()))
                .add("activeEnergyImportKwh",jsonNumberOrNull(input.activeEnergyImportKwh())).add("activeEnergyExportKwh",jsonNumberOrNull(input.activeEnergyExportKwh()))
                .add("voltageL1V",jsonNumberOrNull(input.voltageL1V())).add("voltageL2V",jsonNumberOrNull(input.voltageL2V())).add("voltageL3V",jsonNumberOrNull(input.voltageL3V()))
                .add("currentL1A",jsonNumberOrNull(input.currentL1A())).add("currentL2A",jsonNumberOrNull(input.currentL2A())).add("currentL3A",jsonNumberOrNull(input.currentL3A()))
                .add("frequencyHz",jsonNumberOrNull(input.frequencyHz())).build();
        LiveMeasurementsSocket.broadcast(live.toString());
    }

    private void handleHealth(String topic, JsonObject root, TopicIdentity ti) {
        String clientId = firstText(string(root,"clientId"), ti == null ? null : ti.clientId);
        String siteId = firstText(string(root,"siteId"), ti == null ? null : ti.siteId);
        String edgeId = firstText(string(root,"edgeId"), ti == null ? null : ti.edgeId);
        if (blank(clientId) && !blank(siteId)) clientId = sites.findUniqueBySiteId(siteId).map(s -> s.clientId()).orElse(null);
        if (blank(clientId) || blank(siteId) || blank(edgeId)) return;
        try { edges.ensure(clientId, siteId, edgeId); } catch (Exception ignored) { }
        String status = firstText(string(root,"status"), "ONLINE");
        List<EdgeMqttHealth> mqttTargets = new ArrayList<>(); JsonArray ma=root.getJsonArray("mqttTargets"); if(ma!=null) for(JsonValue v:ma) if(v.getValueType()==JsonValue.ValueType.OBJECT){JsonObject o=v.asJsonObject();mqttTargets.add(new EdgeMqttHealth(string(o,"brokerUri"),bool(o,"connected"),longNumber(o,"publishSuccess"),longNumber(o,"publishErrors")));}
        List<EdgeMeterHealth> mh = new ArrayList<>(); JsonArray mm=root.getJsonArray("meters"); if(mm!=null) for(JsonValue v:mm) if(v.getValueType()==JsonValue.ValueType.OBJECT){JsonObject o=v.asJsonObject();mh.add(new EdgeMeterHealth(string(o,"meterId"),string(o,"serialPortName"),string(o,"status"),instant(o,"lastStatusAt",null),integerNumber(o,"consecutiveFailures"),longNumber(o,"lastReadDurationMs"),string(o,"message")));}
        Instant observedAt = instant(root,"timestamp",Instant.now());
        edgeHealthRegistry.update(new EdgeHealth(clientId,siteId,edgeId,status,observedAt,instant(root,"edgeStartedAt",null),longNumber(root,"uptimeSeconds"),longNumber(root,"systemUptimeSeconds"),doubleNumber(root,"cpuLoadPercent"),doubleNumber(root,"cpuTemperatureC"),doubleNumber(root,"memoryUsedPercent"),longNumber(root,"memoryTotalBytes"),longNumber(root,"memoryFreeBytes"),doubleNumber(root,"diskUsedPercent"),longNumber(root,"diskTotalBytes"),longNumber(root,"diskFreeBytes"),string(root,"hostname"),strings(root.getJsonArray("ipAddresses")),string(root,"version"),strings(root.getJsonArray("serialPorts")),mqttTargets,mh));
        communicationStatusService.recordEdge(clientId, siteId, edgeId, status, observedAt);
        for (EdgeMeterHealth meterHealth : mh) {
            communicationStatusService.recordMeter(clientId, siteId, edgeId, meterHealth.meterId(), meterHealth.status(),
                    observedAt);
        }
    }

    private void handleConfigState(String topic, JsonObject root, TopicIdentity ti) {
        String clientId = firstText(string(root,"clientId"), ti == null ? null : ti.clientId);
        String siteId = firstText(string(root,"siteId"), ti == null ? null : ti.siteId);
        String edgeId = firstText(string(root,"edgeId"), ti == null ? null : ti.edgeId);
        if (blank(clientId) && !blank(siteId)) clientId = sites.findUniqueBySiteId(siteId).map(s -> s.clientId()).orElse(null);
        if (!blank(clientId) && !blank(siteId) && !blank(edgeId)) {
            try { edges.ensure(clientId, siteId, edgeId); } catch (Exception ignored) { }
            edgeConfigurationRegistry.update(clientId,siteId,edgeId,root);
            if (!root.getBoolean("includeRawObis", false)) clearRawMeasurementData(clientId, siteId, edgeId);
        }
    }

    private void handleStatus(String topic, JsonObject root, TopicIdentity ti) {
        String clientId = firstText(string(root,"clientId"), ti == null ? null : ti.clientId);
        String siteId = firstText(string(root,"siteId"), ti == null ? null : ti.siteId);
        String edgeId = firstText(string(root,"edgeId"), ti == null ? null : ti.edgeId);
        String key = firstText(string(root,"meterId"), ti == null ? null : ti.meterId);
        if (blank(siteId)||blank(edgeId)||blank(key)) return;
        Optional<MeterConfiguration> meter = blank(clientId)?meters.findByLegacyIdentity(siteId,edgeId,key):meters.findByIdentity(clientId,siteId,edgeId,key);
        if(meter.isEmpty())return;
        String status = firstText(string(root,"status"),"OFFLINE");
        Instant observedAt = instant(root,"timestamp",Instant.now());
        communicationStatusService.recordMeter(meter.get().clientId(), meter.get().siteId(), meter.get().edgeId(), key, status, observedAt);
        JsonObject live=Json.createObjectBuilder().add("messageType","meter.status").add("meterId",meter.get().id()).add("clientId",meter.get().clientId()).add("siteId",meter.get().siteId()).add("edgeId",meter.get().edgeId()).add("meterUniqueKey",key).add("status",status).add("timestamp",observedAt.toString()).build();
        LiveMeasurementsSocket.broadcast(live.toString());
    }

    private record TopicIdentity(String clientId,String siteId,String edgeId,String meterId) {
        static TopicIdentity from(String topic){
            if(topic==null)return null;
            String[] p=topic.split("/");
            if(p.length<4||!"powerguard".equals(p[0]))return null;
            if(topic.endsWith("/telemetry") || topic.endsWith("/status")) {
                if(p.length==6)return new TopicIdentity(p[1],p[2],p[3],p[4]);
                if(p.length==5)return new TopicIdentity(null,p[1],p[2],p[3]);
            }
            if(topic.endsWith("/health")) {
                if(p.length==5)return new TopicIdentity(p[1],p[2],p[3],null);
                if(p.length==4)return new TopicIdentity(null,p[1],p[2],null);
            }
            if(topic.endsWith("/config/state")) {
                if(p.length==6)return new TopicIdentity(p[1],p[2],p[3],null);
                if(p.length==5)return new TopicIdentity(null,p[1],p[2],null);
            }
            return null;
        }
    }

    private static boolean blank(String s){return s==null||s.isBlank();}
    private static String firstText(String...v){for(String s:v)if(s!=null&&!s.isBlank())return s;return null;}
    private static JsonObject object(JsonObject p,String k){if(p==null||p.isNull(k))return JsonValue.EMPTY_JSON_OBJECT;JsonValue v=p.get(k);return v!=null&&v.getValueType()==JsonValue.ValueType.OBJECT?v.asJsonObject():JsonValue.EMPTY_JSON_OBJECT;}
    private static String string(JsonObject o,String k){if(o==null||!o.containsKey(k)||o.isNull(k))return null;try{return o.getString(k);}catch(Exception e){return String.valueOf(o.get(k));}}
    private static BigDecimal number(JsonObject o,String k){if(o==null||!o.containsKey(k)||o.isNull(k))return null;try{return o.getJsonNumber(k).bigDecimalValue();}catch(Exception e){return null;}}
    private static BigDecimal firstNumber(JsonObject o,String a,String b){BigDecimal v=number(o,a);return v!=null?v:number(o,b);} private static Double doubleNumber(JsonObject o,String k){if(o==null||!o.containsKey(k)||o.isNull(k))return null;try{return o.getJsonNumber(k).doubleValue();}catch(Exception e){return null;}} private static Long longNumber(JsonObject o,String k){if(o==null||!o.containsKey(k)||o.isNull(k))return null;try{return o.getJsonNumber(k).longValue();}catch(Exception e){return null;}} private static Integer integerNumber(JsonObject o,String k){Long v=longNumber(o,k);return v==null?null:v.intValue();} private static Boolean bool(JsonObject o,String k){if(o==null||!o.containsKey(k)||o.isNull(k))return null;try{return o.getBoolean(k);}catch(Exception e){return null;}}
    private static List<String> strings(JsonArray a){if(a==null)return List.of();List<String>x=new ArrayList<>();for(JsonValue v:a)if(v.getValueType()==JsonValue.ValueType.STRING)x.add(((JsonString)v).getString());return List.copyOf(x);} private static Instant instant(JsonObject o,String k,Instant f){String v=string(o,k);if(blank(v))return f;try{return Instant.parse(v);}catch(Exception e){return f;}} private static JsonValue jsonNumberOrNull(BigDecimal v){return v==null?JsonValue.NULL:Json.createValue(v);} private static String setting(String n,String f){String v=System.getenv(n);if(v==null||v.isBlank())v=System.getProperty(n);return v==null||v.isBlank()?f:v.trim();} private static int intSetting(String n,int f){try{return Integer.parseInt(setting(n,String.valueOf(f)));}catch(Exception e){return f;}}
}
