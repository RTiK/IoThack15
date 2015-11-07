package iot.fhnw.ch.umbrella.dao;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import iot.fhnw.ch.umbrella.UmbrellaRecord;

/**
 * Created by benja on 07-Nov-15.
 */
public class RestUmbrellaDAO extends UmbrellaDAO {
    @Override
    public List<UmbrellaRecord> fetchRecords(long from, long to) {
       return null;
    }
}
