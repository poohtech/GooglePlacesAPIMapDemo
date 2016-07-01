package com.example.user.googleapimapdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.example.googleplacesapidemo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends FragmentActivity {

    private TextView tvPhysicalLocation, tvSearchBtn;

    /* for Map */
    private ScrollView mScrollView;
    private GoogleMap map;
    private SupportMapFragment myMapFragment;
    private ImageView imgMapPin;
    private Location loc;
    private LocationManager LC;
    double latitude;
    double longitude;

    /* for AutoCompleteTextview */
    private AutoCompleteTextView edtSearchLocation;
    private GetAddressProcess getAddressProcess;
    private PlacesTask placesTask;
    private ParserTask parserTask;

    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        edtSearchLocation = (AutoCompleteTextView) findViewById(R.id.edtSearchLocation);
        tvPhysicalLocation = (TextView) findViewById(R.id.tvPhysicalLocation);
        tvSearchBtn = (TextView) findViewById(R.id.tvSearchBtn);
        imgMapPin = (ImageView) findViewById(R.id.imgMapPin);
        mScrollView = (ScrollView) findViewById(R.id.mScrollView);

        initilizeMap();

		/* for AutoCompleteTextview */
        edtSearchLocation.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                if (placesTask != null) {
                    placesTask.cancel(true);
                    placesTask = new PlacesTask();
                    placesTask.execute(s.toString());
                } else {
                    placesTask = new PlacesTask();
                    placesTask.execute(s.toString());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        edtSearchLocation
                .setOnEditorActionListener(new OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                                                  KeyEvent event) {

                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            if (edtSearchLocation.getText().toString().trim()
                                    .length() != 1) {
                                getLocationFromAddress(edtSearchLocation
                                        .getText().toString());
                            }
                        }
                        return false;

                    }
                });

        tvSearchBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        0);
                getLocationFromAddress(edtSearchLocation.getText().toString());
            }
        });

    }

    // Fetches all places from GooglePlaces AutoComplete Web Service
    private class PlacesTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            String input = "";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            // Building the url to the web service
            input = "https://maps.googleapis.com/maps/api/place/autocomplete/json?"
                    + input
                    + "&types=geocode&sensor=false&components=country:IN&key=AIzaSyC_wayqAi5M1n7ZRTnC5eHhCjkH7PNj3Vo";

//            input = "http://maps.google.com/maps/api/geocode/json?address="
//                    + input + "&sensor=false";

            System.out.println(" url :::::: " + input);

            try {
                // Fetching the data from web service in background

                InputStream iStream = null;
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(input);

                    // Creating an http connection to communicate with url
                    urlConnection = (HttpURLConnection) url.openConnection();

                    // Connecting to url
                    urlConnection.connect();

                    // Reading data from url
                    iStream = urlConnection.getInputStream();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(iStream));

                    StringBuffer sb = new StringBuffer();

                    String line = "";
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    data = sb.toString();

                    br.close();

                } catch (Exception e) {
                    // Log.print("Exception while downloading url",
                    // e.toString());
                } finally {
                    iStream.close();
                    urlConnection.disconnect();
                }

                System.out.println(" data :: " + data);
            } catch (Exception e) {
                // Log.print("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Creating ParserTask
            parserTask = new ParserTask();

            // Starting Parsing the JSON string returned by Web Service
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends
            AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jObject;

        @Override
        protected List<HashMap<String, String>> doInBackground(
                String... jsonData) {

            List<HashMap<String, String>> places = null;
            try {
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = parse(jObject);

            } catch (Exception e) {
                // Log.print("Exception", e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            String[] from = new String[]{"description"};
            int[] to = new int[]{android.R.id.text1};

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result,
                    android.R.layout.simple_list_item_1, from, to);

            // Setting the adapter
            edtSearchLocation.setAdapter(adapter);
        }
    }

    private void initilizeMap() {
        if (map == null) {

            map = ((CustomMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.mapFragment)).getMap();

            // Set Map Type
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            // Enable MyLocation Button in the Map
            map.setMyLocationEnabled(true);

            imgMapPin.setVisibility(View.VISIBLE);

            getCurrentLocation();

            ((CustomMapFragment) getSupportFragmentManager().findFragmentById(
                    R.id.mapFragment))
                    .setListener(new CustomMapFragment.OnTouchListener() {
                        @Override
                        public void onTouch() {
                            mScrollView
                                    .requestDisallowInterceptTouchEvent(true);

                            edtSearchLocation.setText("");
                        }
                    });

            map.setOnCameraChangeListener(new OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition arg0) {
                    latitude = arg0.target.latitude;
                    longitude = arg0.target.longitude;

                    getAddressFromLatLong(latitude, longitude);

                }
            });
        }
    }

    /* Get Current Location from location Listener */
    private void getCurrentLocation() {

        if (map.getMyLocation() != null) {
            latitude = (double) map.getMyLocation().getLatitude();
            longitude = (double) map.getMyLocation().getLongitude();
        } else {

            LC = (LocationManager) getSystemService(MainActivity.LOCATION_SERVICE);

            loc = LC.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (loc != null) {
                latitude = (double) loc.getLatitude();
                longitude = (double) loc.getLongitude();
            } else {

                loc = LC.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (loc != null) {
                    loc = LC.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    latitude = (double) loc.getLatitude();
                    longitude = (double) loc.getLongitude();
                } else {
                    // AlertDailogView.showAlert(AddPropertyLocationActivity.this,
                    // getResources().getString(R.string.nolocation),
                    // getResources().getString(R.string.ok), true).show();
                    Toast.makeText(MainActivity.this, "No location",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
        /* Move map & set pin on this Latitude-Longitude */
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                latitude, longitude), 13.8f));
    }

    /* Get latitude & longitude from address */
    public void getLocationFromAddress(String strAddress) {

        Geocoder coder = new Geocoder(MainActivity.this);
        List<Address> address;

        try {
            address = coder.getFromLocationName(strAddress, 5);

            if (address == null) {

            }

            if (address.size() > 0) {
                this.latitude = address.get(0).getLatitude();
                this.longitude = address.get(0).getLongitude();
            }

			/* Move map & set pin on this Latitude-Longitude */
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                    this.latitude, this.longitude), 15));

        } catch (Exception ex) {

            ex.printStackTrace();
        }

    }

    /* START ::::: Get address from latitude & longitude */
    private void getAddressFromLatLong(double argLat, double argLong) {
        try {

            if (Utils.isOnline(MainActivity.this)) {
                if (getAddressProcess != null) {
                    getAddressProcess.cancel(true);
                    getAddressProcess = null;
                }
                if (argLat != 0 && argLong != 0) {
                    map.clear();
                    getAddressProcess = new GetAddressProcess(this, handler, 1,
                            argLat, argLong);
                    getAddressProcess.execute();
                }
            } else {
                // AlertDailogView
                // .showAlert(
                // AddPropertyLocationActivity.this,
                // getResources().getString(
                // R.string.connectionError),
                // getResources().getString(
                // R.string.connectionErrorMessage),
                // getResources().getString(R.string.tryAgain),
                // true, null).show();
            }
        } catch (Exception e) {
            // Log.debug(this.getClass() + " ADD LOCATION", " Exception ::: " +
            // e);
        }
    }

    class GetAddressProcess extends AsyncTask<Void, Void, Integer> {
        public int tag;
        public double lat, lon;
        public Context caller;
        public Handler handler;
        public String address;

        public GetAddressProcess(Context caller, Handler handler, int tag,
                                 double lat, double lon) {
            this.tag = tag;
            this.caller = caller;
            this.handler = handler;
            this.lat = lat;
            this.lon = lon;
        }

        protected void onPreExecute() {
        }

        protected Integer doInBackground(Void... arg0) {
            int result = 200;
            String uri = "http://maps.google.com/maps/api/geocode/json?address="
                    + lat + "," + lon + "&sensor=false";
            HttpGet httpGet = new HttpGet(uri);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            StringBuilder stringBuilder = new StringBuilder();
            int b;

            try {
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                InputStream stream = entity.getContent();

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    while ((b = stream.read()) != -1) {
                        stringBuilder.append((char) b);
                    }

                    address = this.parse(stringBuilder.toString());
                }
            } catch (ClientProtocolException e) {
                result = -1;
                e.printStackTrace();
            } catch (IOException e) {
                result = -1;
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute(Integer result) {
            Message msg = new Message();

            msg.what = this.tag;
            msg.arg1 = result;
            msg.obj = address;

            this.handler.sendMessage(msg);
        }

        public String parse(String response) {
            String address = null;
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(response);

                address = ((JSONArray) jsonObject.get("results"))
                        .getJSONObject(0).getString("formatted_address");
            } catch (JSONException e) {
                address = null;
                e.printStackTrace();
            }
            jsonObject = null;
            return address;
        }
    }

    // InsertData Process Handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String addressInfo = null;

            try {
                if (msg.what == 1 && msg.arg1 == 200) {
                    addressInfo = (String) msg.obj;
                    /* Set address in TextView */
                    tvPhysicalLocation.setText(addressInfo);

                }
            } catch (Exception e) {
                // Log.debug(this.getClass() + " :: ", " handler " + msg);
            }
            addressInfo = null;
        }
    };

	/* END ::::: Get address from latitude & longitude */

    /**
     * Receives a JSONObject and returns a list
     */
    public List<HashMap<String, String>> parse(JSONObject jObject) {

        JSONArray jPlaces = null;
        try {
            /** Retrieves all the elements in the 'places' array */
            jPlaces = jObject.getJSONArray("predictions");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /**
         * Invoking getPlaces with the array of json object where each json
         * object represent a place
         */
        return getPlaces(jPlaces);
    }

    private List<HashMap<String, String>> getPlaces(JSONArray jPlaces) {
        int placesCount = jPlaces.length();
        List<HashMap<String, String>> placesList = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> place = null;

        /** Taking each place, parses and adds to list object */
        for (int i = 0; i < placesCount; i++) {
            try {
                /** Call getPlace with place JSON object to parse the place */
                place = getPlace((JSONObject) jPlaces.get(i));
                placesList.add(place);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return placesList;
    }

    /**
     * Parsing the Place JSON object
     */
    private HashMap<String, String> getPlace(JSONObject jPlace) {

        HashMap<String, String> place = new HashMap<String, String>();

        String id = "";
        String reference = "";
        String description = "";

        try {

            description = jPlace.getString("description");
            id = jPlace.getString("id");
            reference = jPlace.getString("reference");

            place.put("description", description);
            place.put("_id", id);
            place.put("reference", reference);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return place;
    }

}
