package iot.fhnw.ch.umbrella.dao;


import java.util.List;

import iot.fhnw.ch.umbrella.UmbrellaRecord;

public abstract class UmbrellaDAO {

    /**
     * @param from
     * @param to
     * @return
     */
    public abstract List<UmbrellaRecord> fetchRecords(long from, long to);

    public final UmbrellaRecord parseRecord(String line) {

        final String[] tokens =  line.split(",");
        final long id = Long.valueOf(tokens[0]);
        final double heat = Double.valueOf(tokens[1]);
        final double volume = Double.valueOf(tokens[2]);
        final long timestamp = Long.valueOf(tokens[3]);
        final double lat = Double.valueOf(tokens[4]);
        final double lng = Double.valueOf(tokens[5]);

        final int eventType = Integer.valueOf(tokens[6]);

        final UmbrellaRecord.EventType type;
        switch (eventType) {
            case 0: type = UmbrellaRecord.EventType.OPENED; break;
            case 1: type = UmbrellaRecord.EventType.OPEN; break;
            case 2: type = UmbrellaRecord.EventType.CLOSED; break;
            default: type = UmbrellaRecord.EventType.OPEN; break;
        }

        return new UmbrellaRecord(id,  heat, volume, timestamp, lat, lng, type);
    }
}
