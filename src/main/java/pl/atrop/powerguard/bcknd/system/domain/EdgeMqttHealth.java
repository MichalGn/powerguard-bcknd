package pl.atrop.powerguard.bcknd.system.domain;

public record EdgeMqttHealth(
        String brokerUri,
        Boolean connected,
        Long publishSuccess,
        Long publishErrors) { }
