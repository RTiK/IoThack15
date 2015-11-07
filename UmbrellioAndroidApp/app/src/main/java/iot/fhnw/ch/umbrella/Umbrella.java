package iot.fhnw.ch.umbrella;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.LinkedList;


public class Umbrella {
    public final long id;
    public final long startTimeStamp;
    public final double heat;
    public final double volume;

    public LatLng position;
    public Marker marker;

    private LinkedList<Step> targets = new LinkedList<>();

    private Step currentStep;
    private long currentStepTime = 0;
    private double deltaLat;
    private double deltaLng;

    public Umbrella(long id, LatLng position, final long startTimeStamp, final double heat, final double volume) {
        this.id = id;
        this.position = position;
        this.startTimeStamp = startTimeStamp;
        this.heat = heat;
        this.volume = volume;
    }

    public void setMarker(final Marker marker) {
        this.marker = marker;
    }

    public synchronized void pushPosition(LatLng nextPosition, long timeStamp) {
        final long duration = targets.size() > 0 ? timeStamp - targets.getLast().timeStep : timeStamp - startTimeStamp;
        targets.add(new Step(nextPosition, duration, timeStamp));
    }

    public void updatePosition(long deltaT) {
        synchronized (this) {
            if (targets.size() > 0) {
                // reset
                if (targets.size() > 0 && currentStep == null || currentStepTime > currentStep.duration) {
                    currentStep = targets.poll();
                    currentStepTime = 0;
                    deltaLat = currentStep.position.latitude - position.latitude;
                    deltaLng = currentStep.position.longitude - position.longitude;
                }

                // interpolate position
                currentStepTime += deltaT;

                final double stepLat = deltaLat / currentStep.duration * deltaT;
                final double stepLng = deltaLng / currentStep.duration * deltaT;

                position = new LatLng(position.latitude + stepLat, position.longitude + stepLng);
                marker.setPosition(position);
            }

        }


    }

    private static final class Step {
        public final LatLng position;
        public final long duration;
        public final long timeStep;


        public Step(LatLng nextPosition, long duration, long timeStep) {
            this.position = nextPosition;
            this.duration = duration;
            this.timeStep = timeStep;
        }
    }
}
