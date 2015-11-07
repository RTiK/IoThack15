package iot.fhnw.ch.umbrella;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import iot.fhnw.ch.umbrella.dao.LocalUmbrellaDAO;
import iot.fhnw.ch.umbrella.dao.UmbrellaDAO;


// TODO read positions from stream
// TODO create vector field based on positions over time

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap maps;


    private InputMode mode = InputMode.NONE;

    private UmbrellaDAO localDao;
    private UmbrellaDAO liveDao;
    private volatile UmbrellaDAO dao;

    private MenuItem liveMenuItem;
    private MenuItem pastMenuItem;
    private MenuItem inputMenuItem;


    private UmbrellaProcessor umbrellaProcessor;
    private ScheduledExecutorService umbrellaPollerService = Executors.newScheduledThreadPool(1);

    private Map<Long, Umbrella> umbrellas = new HashMap<>();
    private List<GroundOverlay> overlays = new ArrayList<>();

    private TileOverlay heatOverlay;
    private TileOverlay volumeOverlay;

    public static final int K = 8;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // create daos
        localDao = new LocalUmbrellaDAO(getApplicationContext().getResources().openRawResource(R.raw.testdata2));
        liveDao = null;
        dao = localDao;


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        inputMenuItem = menu.findItem(R.id.action_input_mode);
        pastMenuItem = menu.findItem(R.id.action_past_mode);
        liveMenuItem = menu.findItem(R.id.action_live_mode);

        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.maps = googleMap;

        this.maps.setMyLocationEnabled(true);

        this.maps.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                System.out.println(latLng);
            }
        });

        // add initial overlays which will later be used
        for (int i = 0; i < K; ++i) {
            final GroundOverlayOptions overlay = new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromResource(R.drawable.cloud1))
                    .position(new LatLng(0, 0), 6000f, 6000f);

            overlays.add(googleMap.addGroundOverlay(overlay));
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_input_mode:
                dao = null;
                mode = InputMode.INPUT;
                break;
            case R.id.action_past_mode:
                dao = localDao;
                mode = InputMode.PAST;
                break;
            case R.id.action_live_mode:
                dao = liveDao;
                mode = InputMode.REAL;
                break;
            case R.id.action_heatmap: {
                if (heatOverlay != null) {
                    heatOverlay.remove();
                    heatOverlay = null;
                    return true;
                }

                // add heat heatOverlay
                final List<WeightedLatLng> data = new ArrayList<>(umbrellas.size());
                double maxHeat = 0;
                for (Umbrella umbrella : umbrellas.values()) {
                     if (maxHeat <  umbrella.heat) {
                         maxHeat = umbrella.heat;

                     }
                }

                // normalize data
                for (Umbrella umbrella : umbrellas.values()) {
                    data.add(new WeightedLatLng(umbrella.position, umbrella.heat / maxHeat));
                }

                // add heat tile
                HeatmapTileProvider tileProvider = new HeatmapTileProvider.Builder().weightedData(data).radius(50).opacity(0.3).build();
                heatOverlay = maps.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
                break;
            }
            case R.id.action_volumemap: {
                if (volumeOverlay != null) {
                    volumeOverlay.remove();
                    volumeOverlay = null;
                    return true;
                }

                // add heat heatOverlay
                final List<WeightedLatLng> data = new ArrayList<>(umbrellas.size());
                double maxVolume = 0;
                for (Umbrella umbrella : umbrellas.values()) {
                    if (maxVolume <  umbrella.volume) {
                        maxVolume = umbrella.volume;

                    }
                }

                int[] colors = {
                        Color.rgb(70, 0, 215), // green
                        Color.rgb(255, 0, 0)    // red
                };

                float[] startPoints = {
                        0.2f, 1f
                };

                Gradient gradient = new Gradient(colors, startPoints);


                // normalize data
                for (Umbrella umbrella : umbrellas.values()) {
                    data.add(new WeightedLatLng(umbrella.position, umbrella.volume / maxVolume));
                }



                // add heat tile
                HeatmapTileProvider tileProvider = new HeatmapTileProvider.Builder().gradient(gradient).weightedData(data).radius(50).opacity(0.3).build();
                volumeOverlay = maps.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
                break;
            }


        }

        uncheckGroup();
        item.setChecked(true);

        if (item.getItemId() == R.id.action_live_mode || item.getItemId() == R.id.action_past_mode) {
            umbrellaPollerService.scheduleAtFixedRate(new UmbrellaPoller(), 0, 1000, TimeUnit.MILLISECONDS);

            // TODO shutdown correctly
            /*
            if (umbrellaProcessor != null) {
                umbrellaProcessor.processUmbrellas = false;
                umbrellaProcessor.interrupt();
            }
            */

            umbrellaProcessor = new UmbrellaProcessor();
            umbrellaProcessor.start();
        }


        return true;
    }

    private void uncheckGroup() {
        liveMenuItem.setChecked(false);
        pastMenuItem.setChecked(false);
        inputMenuItem.setChecked(false);
    }

    public enum InputMode {
        INPUT, PAST, REAL, NONE
    }

    private class UmbrellaProcessor extends Thread {

        public volatile boolean processUmbrellas = true;

        @Override
        public void run() {
            long time = System.currentTimeMillis();
            long delta = 0;
            while (processUmbrellas) {
                // update umbrella positions
                synchronized (umbrellas) {
                    for (final Umbrella umbrella : umbrellas.values()) {
                        final long deltaHandle = delta;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                umbrella.updatePosition(deltaHandle);
                            }
                        });

                    }
                }


                // add cloud overlays

                final List<Cluster> clusters = clusterUmbrellas(umbrellas);

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        List<GroundOverlay> availableOverlays = new ArrayList<>(overlays);
                        for (final Cluster cluster : clusters) {
                            GroundOverlay nearest = null;
                            double nearestDist = Double.MAX_VALUE;
                            for (GroundOverlay overlay : availableOverlays) {
                                LatLng position = overlay.getPosition();
                                double dist = latlngDistance(position, cluster.centroid.position);
                                if (dist < nearestDist) {
                                    nearestDist = dist;
                                    nearest = overlay;
                                }
                            }

                            availableOverlays.remove(nearest);

                            nearest.setPosition(cluster.centroid.position);
                        }
                    }
                });


                try {
                    Thread.sleep(32);
                } catch (InterruptedException e) {
                    // ignore
                }

                delta = System.currentTimeMillis() - time;
                time = System.currentTimeMillis();
            }

        }
    }

    private static double latlngDistance(LatLng first, LatLng second) {
        double deltaLat = first.latitude - second.latitude;
        double deltaLng = first.longitude - second.longitude;

        return Math.sqrt((deltaLat * deltaLat + deltaLng * deltaLng));
    }

    private List<Cluster> clusterUmbrellas(Map<Long, Umbrella> umbrellaMap) {
        if (umbrellaMap.isEmpty()) return Collections.emptyList();


        final List<Umbrella> umbrellas = new ArrayList<>(umbrellaMap.values());
        final List<Cluster> clusters = new ArrayList<>();

        final List<Point> points = new ArrayList<>();

        for (Umbrella umbrella : umbrellas) {
            points.add(new Point(umbrella.position));
        }

        // initialize
        for (int clusterIndex = 0; clusterIndex < K; ++clusterIndex) {
            Point center = points.get(clusterIndex);
            center.clusterIndex = clusterIndex;

            clusters.add(new Cluster(center));
        }

        boolean changed = false;
        int steps = 10;
        while (!changed && steps > 0) {

            for (Cluster cluster : clusters) {
                cluster.points.clear();
            }


            // assign
            for (Point point : points) {

                int nearest = 0;
                double nearestDist = Double.MAX_VALUE;

                for (int clusterIndex = 0; clusterIndex < clusters.size(); ++clusterIndex) {
                    Cluster cluster = clusters.get(clusterIndex);

                    double dist = cluster.centroid.distance(point);
                    if (dist < nearestDist) {
                        nearest = clusterIndex;
                        nearestDist = dist;
                    }
                }

                if (point.clusterIndex != nearest) {
                    changed = true;
                }

                point.clusterIndex = nearest;
                clusters.get(nearest).points.add(point);

            }

            // reorder centroids
            for (Cluster cluster : clusters) {
                cluster.updateCentroid();
            }


            steps--;
        }


        return clusters;
    }

    private class Cluster {
        private List<Point> points = new ArrayList<>();
        private Point centroid;

        public Cluster(Point centroid) {
            this.centroid = centroid;
        }

        public void updateCentroid() {
            double lng = 0;
            double lat = 0;
            for (Point point : points) {
                lng += point.position.longitude;
                lat += point.position.latitude;
            }

            centroid = new Point(new LatLng(lat / points.size(), lng / points.size()));

        }
    }

    private class Point {
        public int clusterIndex = -1;
        public final LatLng position;

        public Point(LatLng position) {
            this.position = position;
        }

        public double distance(Point other) {
            double deltaLat = position.latitude - other.position.latitude;
            double deltaLng = position.longitude - other.position.longitude;

            return Math.sqrt((deltaLat * deltaLat + deltaLng * deltaLng));
        }
    }

    private class UmbrellaPoller implements Runnable {


        private long timeStamp = 0;

        @Override
        public void run() {
            // poll next umbrellas
            List<UmbrellaRecord> newRecords = dao.fetchRecords(timeStamp, System.currentTimeMillis());
            timeStamp = System.currentTimeMillis();

            for (final UmbrellaRecord newRecord : newRecords) {

                switch (newRecord.type) {
                    case OPENED: {

                        final LatLng position = new LatLng(newRecord.latitude, newRecord.longitude);
                        final Umbrella umbrella = new Umbrella(newRecord.id, position, newRecord.timestamp, newRecord.heat, newRecord.volume);
                        umbrellas.put(newRecord.id, umbrella);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (umbrellas) {
                                    final Marker marker = maps.addMarker(new MarkerOptions().position(position).title("Umbrella: " + newRecord.id).icon(BitmapDescriptorFactory.fromResource(R.drawable.umbrella3)));
                                    umbrella.setMarker(marker);

                                }
                            }
                        });

                        break;
                    }
                    case OPEN: {
                        umbrellas.get(newRecord.id).pushPosition(new LatLng(newRecord.latitude, newRecord.longitude), newRecord.timestamp);
                        break;
                    }
                    case CLOSED: {
                        //final Umbrella umbrella = umbrellas.get(newRecord.id);
                        //umbrella.marker.remove();
                        //umbrellas.remove(newRecord.id);
                    }


                }
            }
        }
    }
}
