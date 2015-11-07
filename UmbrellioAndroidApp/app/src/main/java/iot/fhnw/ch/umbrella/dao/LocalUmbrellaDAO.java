package iot.fhnw.ch.umbrella.dao;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import iot.fhnw.ch.umbrella.UmbrellaRecord;

public class LocalUmbrellaDAO extends UmbrellaDAO {

    private List<UmbrellaRecord> records = new LinkedList<>();

    public LocalUmbrellaDAO(InputStream stream) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    records.add(parseRecord(line));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(records, new Comparator<UmbrellaRecord>() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @Override
            public int compare(UmbrellaRecord lhs, UmbrellaRecord rhs) {
                return Long.compare(lhs.timestamp, rhs.timestamp);
            }
        });
    }


    @Override
    public List<UmbrellaRecord> fetchRecords(long from, long to) {
        final List<UmbrellaRecord> result = new ArrayList<>();

        for (UmbrellaRecord record : records) {
            if (record.timestamp >= from && record.timestamp <= to) {
                result.add(record);
            }
        }
        return result;


    }
}
