package iot.fhnw.ch.umbrella;

public class UmbrellaRecord {
    public final long id;
    public final double heat;
    public final double volume;
    public final long timestamp;
    public final double latitude;
    public final double longitude;
    public final EventType type;

    public UmbrellaRecord(long id, double heat, double volume,  long timestamp, double latitude, double longitude, EventType type) {
        this.id = id;
        this.heat = heat;
        this.volume = volume;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
        this.type = type;
    }

    public enum EventType {
        OPENED, OPEN, CLOSED
    }
}
