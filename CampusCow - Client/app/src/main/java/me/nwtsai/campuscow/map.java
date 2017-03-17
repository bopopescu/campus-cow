package me.nwtsai.campuscow;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import android.os.Handler;
import android.app.Dialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import im.delight.android.location.SimpleLocation;

public class map extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener
{
    // Google Map variables
    private GoogleMap myMap;
    private GoogleApiClient client;
    private static final String TAG = map.class.getSimpleName();

    // Current player's index, pulled from the previous activity
    private int currentPlayerIndex = -1;

    // Use this to get the player's current location
    private SimpleLocation location;

    // For running a method every TIME_INTERVAL ms
    private Handler handler = new Handler();
    private final int TIME_INTERVAL = 5000;

    // For periodically showing the cow marker
    private Handler showCowHandler = new Handler();
    private int COW_TIME_INTERVAL = 15 * 1000;
    private boolean showCowFlag = false;

    // Game Over flag
    private boolean game_over;

    // Instance of map fragment
    private SupportMapFragment mapFragment;

    // API URL for communicating with the server
    private String BASE_URL = "http://campuscow.us-east-1.elasticbeanstalk.com/";
    private String API_URL = "api/";
    private String EMPTY = "empty";

    // A HashMap that maps a player's id to each player's marker
    private HashMap<Integer, Marker> markerMap = new HashMap<Integer, Marker>();

    // A HashMap that maps a player's id to each player's LatLng
    private HashMap<Integer, LatLng> positionMap = new HashMap<Integer, LatLng>();

    // A HashMap that maps a player's id to each player's distance from the cow
    private HashMap<Integer, Double> distanceFromCowMap = new HashMap<Integer, Double>();
    
    // When the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Grab the player index passed by the home screen
        Intent iin = getIntent();
        Bundle b = iin.getExtras();

        // Find the current index of the player and save it into a private variable
        if (b != null)
        {
            // Grab the player index from the previous activity
            currentPlayerIndex = (int) b.get("playerIndex");
        }

        // Construct a new instance of SimpleLocation
        location = new SimpleLocation(this);

        // If we can't access the location yet
        if (!location.hasLocationEnabled())
        {
            // Ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        // Always set the game_over flag to false when the onCreate of this activity gets called
        game_over = false;

        // Call onMapReady
        mapFragment.getMapAsync(this);
    }

    // When the map is ready, run this block of code
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        // Google Maps and initial settings
        myMap = googleMap;
        myMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        myMap.setMinZoomPreference(15.0f);
        myMap.setMaxZoomPreference(20.0f);

        // Set the boundary of the map (UCLA Campus)
        LatLngBounds UCLABorder = new LatLngBounds(new LatLng(34.06238352853008,-118.4531307220459), new LatLng(34.07974903895123,-118.43613624572754));
        myMap.setLatLngBoundsForCameraTarget(UCLABorder);

        // New instance
        location = new SimpleLocation(this);

        // Create a marker for the current player depending on if the person is a cow or cowboy
        LatLng YourPosition;
        YourPosition = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions YourMarkerOptions;
        Marker YourMarker;
        Double distanceFromCow;

        // If the player index is 0, make a marker with a campus cow icon and a Campus Cow title
        if (currentPlayerIndex == 0)
        {
            // Add the Campus Cow with a cow icon
            distanceFromCow = 0.0;
            YourMarkerOptions = new MarkerOptions().position(YourPosition).title("Campus Cow (YOU)")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.campus_cow));
        }

        // If the current player is not the cow AND no cow exists on the map yet, create a blue marker at the current location
        else
        {
            // Set a default color of red and default distanceFromCow if a cow doesn't exist yet. Volley will update it later.
            distanceFromCow = 601.0;
            YourMarkerOptions = new MarkerOptions().position(YourPosition).title("Cowboy " + Integer.toString(currentPlayerIndex) + " (YOU)")
                    .icon(BitmapDescriptorFactory.defaultMarker(generateMarkerColor(distanceFromCow)));
        }

        // Add the marker onto the map
        YourMarker = myMap.addMarker(YourMarkerOptions);

        // Add values into the maps based on currentPlayerIndex
        markerMap.put(currentPlayerIndex, YourMarker);
        positionMap.put(currentPlayerIndex, YourPosition);
        distanceFromCowMap.put(currentPlayerIndex, distanceFromCow);

        // Center the camera around the current player and programmatically select the marker
        markerMap.get(currentPlayerIndex).showInfoWindow();
        myMap.animateCamera(CameraUpdateFactory.newLatLng(markerMap.get(currentPlayerIndex).getPosition()), 250, null);

        // Customize the map with a json file found in the raw folder
        try
        {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
            if (!success)
            {
                Log.e(TAG, "Style parsing failed.");
            }
        }
        catch (Resources.NotFoundException e)
        {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Refresh the map until the game ends
        refreshMap();

        // Only show/hide cow if the current player is a cowboy
        if (currentPlayerIndex != 0)
        {
            // Show the cow periodically
            showCowPeriodically();
        }
    }

    // Refresh the map every TIME_INTERVAL seconds
    public void refreshMap()
    {
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                // Update the positionArray variable if the current location is available
                if (location != null)
                {
                    getAllPositionsFromJSONArray();
                }

                // Update the distance from cow ArrayList once the new positions are stored
                updateDistancesFromCow();

                // Update the positions of every marker in markerArray
                updateAllMarkerPositions();

                // Update the marker colors of every cowboy
                updateAllCowBoyMarkerColors();

                // Find the winner
                Integer winner = whoFoundCow();

                // If a winner exists
                if (winner != -1)
                {
                    game_over = true;
                    emptyServerData();
                    stopGameAlert(winner);
                }

                // Log the current position of the device
                // Log.e("Current Loc", location.getPosition().toString());

                // Print distance between device and cow
                if (currentPlayerIndex != 0 && positionMap.get(0) != null)
                {
                    Log.e("Distance", Double.toString(distanceFromCowMap.get(1)));
                }

                // Call this method again if the game isn't over
                if (game_over == false)
                {
                    handler.postDelayed(this, TIME_INTERVAL);
                }

                // If the game is over, remove all callbacks and messages
                else
                {
                    handler.removeCallbacksAndMessages(null);
                }
            }
        }, TIME_INTERVAL);
    }

    // Show the cow periodically
    public void showCowPeriodically()
    {
        showCowHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                // If a cow marker exists, show it for 5 seconds
                if (markerMap.get(0) != null && positionMap.get(0) != null)
                {
                    // Toggle the show cow flag
                    showCowFlag = !showCowFlag;

                    // Change the timer based on the flag
                    if (showCowFlag)
                    {
                        markerMap.get(0).setPosition(positionMap.get(0));
                        COW_TIME_INTERVAL = 5 * 1000;
                        Log.e("Cow Flag", "True");
                    }
                    else
                    {
                        markerMap.get(0).setPosition(new LatLng(0.0, 0.0));
                        COW_TIME_INTERVAL = 15 * 1000;
                        Log.e("Cow Flag", "False");
                    }
                }

                // Call this method again if the game isn't over
                if (game_over == false)
                {
                    showCowHandler.postDelayed(this, COW_TIME_INTERVAL);
                }

                // If the game is over, remove all callbacks and messages
                else
                {
                    showCowHandler.removeCallbacksAndMessages(null);
                }
            }
        }, COW_TIME_INTERVAL);
    }

    // Update the position map if parsing a JSONArray
    public void getAllPositionsFromJSONArray()
    {
        location.beginUpdates();
        Log.e("Current Loc", location.getPosition().toString());

        // Send the current user's location to the server
        String myLat = Double.toString(location.getLatitude());
        String myLon = Double.toString(location.getLongitude());
        String tempURL = BASE_URL + API_URL + Integer.toString(currentPlayerIndex) + "," + myLat + "," + myLon;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(tempURL, new Response.Listener<JSONArray>()
        {
            @Override
            public void onResponse(JSONArray myJSOnArray)
            {
                // Load the positionArray for every JSONObject received
                for(int i = 0; i < myJSOnArray.length(); i++)
                {
                    try
                    {
                        JSONObject jsonObject = myJSOnArray.getJSONObject(i);
                        Integer currID = jsonObject.getInt("id");
                        Double currLat = jsonObject.getDouble("lat");
                        Double currLon = jsonObject.getDouble("lon");
                        LatLng currLatLng = new LatLng(currLat, currLon);

                        // Put the position into the map
                        positionMap.put(currID, currLatLng);
                    }
                    catch (JSONException e)
                    {
                        Log.e("JSON Err", e.toString());
                    }
                }
            }
        },
        new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError volleyError)
            {
                Log.e("Error", "Can't parse JSONArray");
            }
        });
        queue.add(jsArrayRequest);
    }

    // Update all marker positions
    public void updateAllMarkerPositions()
    {
        for (Map.Entry<Integer, LatLng> entry : positionMap.entrySet())
        {
            // If a marker doesn't exist for a specific user ID, create a new marker with the correct color and add it to map
            if (markerMap.get(entry.getKey()) == null && entry.getKey() != 0)
            {
                MarkerOptions myMarkerOptions = new MarkerOptions().position(entry.getValue()).title("Cowboy " + Integer.toString(entry.getKey()));

                // Only if a cow does exist
                if (positionMap.get(0) != null)
                {
                    myMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(generateMarkerColor(distanceFromCowMap.get(entry.getKey()))));
                }
                else
                {
                    myMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(generateMarkerColor(601.0)));
                }
                Marker myMarker = myMap.addMarker(myMarkerOptions);
                markerMap.put(entry.getKey(), myMarker);
            }

            // If a cow position is valid and a cow just got added
            else if (markerMap.get(entry.getKey()) == null && positionMap.get(0) != null && entry.getKey() == 0)
            {
                MarkerOptions myMarkerOptions = new MarkerOptions().position(entry.getValue()).title("Campus Cow")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.campus_cow));
                Marker myMarker = myMap.addMarker(myMarkerOptions);
                markerMap.put(0, myMarker);

                // If the current player is not the cow, set position of cow marker to be off the map
                if (currentPlayerIndex != 0)
                {
                    markerMap.get(0).setPosition(new LatLng(0.0, 0.0));
                }
            }

            // If a marker already exists on the map, just set the position of the marker with a new position value
            else if (markerMap.get(entry.getKey()) != null)
            {
                // If the marker we want to update is a cow AND I'm not the cow
                if (entry.getKey() == 0 && currentPlayerIndex != 0)
                {
                    if (showCowFlag)
                    {
                        markerMap.get(0).setPosition(positionMap.get(0));
                    }
                    else
                    {
                        markerMap.get(0).setPosition(new LatLng(0.0, 0.0));
                    }
                }

                // If the marker is not a cow (works if I'm cow or if I'm not cow)
                else
                {
                    markerMap.get(entry.getKey()).setPosition(entry.getValue());
                }
            }
        }
    }

    // Update all the distances from the cow, don't worry about 0 index, it is always 0.0
    public void updateDistancesFromCow()
    {
        // If a cow exists that isn't the current player
        if (positionMap.get(0) != null)
        {
            for (Map.Entry<Integer, LatLng> entry : positionMap.entrySet())
            {
                if (entry.getKey() != 0)
                {
                    Double cowLat = positionMap.get(0).latitude;
                    Double cowLong = positionMap.get(0).longitude;
                    Double currentLat = entry.getValue().latitude;
                    Double currentLong = entry.getValue().longitude;
                    Double currentDistanceFromCow = location.calculateDistance(cowLat, cowLong, currentLat, currentLong);
                    distanceFromCowMap.put(entry.getKey(), currentDistanceFromCow);
                }
                else
                {
                    distanceFromCowMap.put(0, 0.0);
                }
            }
        }
    }

    // Update all cowboy marker colors depending on how far away each cowboy is from the cow
    public void updateAllCowBoyMarkerColors()
    {
        for (Map.Entry<Integer, Marker> entry : markerMap.entrySet())
        {
            // If the marker isn't for the cow change the color of the marker and a cow exists
            if (entry.getKey() != 0 && positionMap.get(0) != null)
            {
                markerMap.get(entry.getKey()).setIcon(BitmapDescriptorFactory.defaultMarker(generateMarkerColor(distanceFromCowMap.get(entry.getKey()))));
            }

            // If the cow doesn't exist
            else if (entry.getKey() != 0)
            {
                markerMap.get(entry.getKey()).setIcon(BitmapDescriptorFactory.defaultMarker(generateMarkerColor(601.0)));
            }
        }
    }

    // Depending on the distance between the location of a player and the cow, return a marker color
    public float generateMarkerColor(Double distanceFromCow)
    {
        float markerColor;

        // HOT
        if (distanceFromCow < 200)
        {
            markerColor = BitmapDescriptorFactory.HUE_RED;
        }
        // WARM
        else if (distanceFromCow < 400)
        {
            markerColor = BitmapDescriptorFactory.HUE_ORANGE;
        }
        // NEUTRAL
        else if (distanceFromCow < 600)
        {
            markerColor = BitmapDescriptorFactory.HUE_YELLOW;
        }
        // COLD
        else
        {
            markerColor = BitmapDescriptorFactory.HUE_BLUE;
        }

        return markerColor;
    }

    // This function gets called continuously so all users will get notified when the game is over
    // Returns -1 if the cow hasn't been found yet or if a cow doesn't exist
    // Returns the player # of the winner if the cow has been found
    public int whoFoundCow()
    {
        if (positionMap.get(0) == null)
            return -1;

        // Grab the cow's position once for efficiency
        LatLng cowTempPos = positionMap.get(0);

        // Keep track of the closest cowboy index
        Integer closestCowboyIndex = -1;
        Double closestDistanceFromCow = 60.0;

        // Loop through and look for the closest cowboy
        for (Map.Entry<Integer, LatLng> entry : positionMap.entrySet())
        {
            if (entry.getKey() != 0)
            {
                Double cowboyTempLat = entry.getValue().latitude;
                Double cowboyTempLong = entry.getValue().longitude;
                Double distanceTemp = location.calculateDistance(cowTempPos.latitude, cowTempPos.longitude, cowboyTempLat, cowboyTempLong);
                if (distanceTemp <= closestDistanceFromCow)
                {
                    closestCowboyIndex = entry.getKey();
                    closestDistanceFromCow = distanceTemp;
                }
            }
        }

        // Return the index of the closest cowboy
        if (closestCowboyIndex != -1)
        {
            return closestCowboyIndex;
        }
        else
        {
            return -1;
        }
    }

    // Stop running the recurring function and display who the winner is; if the currentPlayerIndex is 0, display the cow loses
    public void stopGameAlert(int playerIndex)
    {
        // Stop the recurring function
        handler.removeCallbacksAndMessages(null);

        // Create a non-cancelable dialog for the winner
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(map.this);

        // If the player is a cowboy, display the winning message
        if (currentPlayerIndex != 0)
            alertBuilder.setTitle("Cowboy " + Integer.toString(playerIndex) + " Wins!");
        else
            alertBuilder.setTitle("You've been found by Cowboy " + Integer.toString(playerIndex));
        alertBuilder.setCancelable(false).setPositiveButton("New Game", new DialogInterface.OnClickListener()
        {
            // When the NEW GAME button is pressed, go back to the home screen
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                transitionToNewActivity(currentPlayerIndex);
                emptyServerData();
            }
        });

        // Create and show the dialog
        Dialog dialog = alertBuilder.create();
        dialog.show();
    }

    // Once NEW GAME is pressed, empty the server data by calling the EMPTY endpoint
    public void emptyServerData()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, BASE_URL + EMPTY, null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                try
                {
                    response.getInt("dummy");
                    Log.e("emptyServerData", "hasRun");
                }
                catch (JSONException e)
                {
                    Log.e("Can't empty server", e.toString());
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                //TO DO Auto generated method stub
                Log.e("Volley ID Error", error.toString());
            }
        });

        queue.add(jsObjRequest);
    }

    // Do something when the marker gets clicked
    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    // Start a new game by going back to the lobby screen
    void transitionToNewActivity(int pI)
    {
        Intent myIntent = new Intent(map.this, homescreen.class);
        myIntent.putExtra("playerIndex", pI);
        map.this.startActivity(myIntent);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop()
    {
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onResume()
    {
        // Make the device update its location
        super.onResume();
        location.beginUpdates();
    }

    @Override
    protected void onPause()
    {
        // Stop location updates (saves battery)
        location.endUpdates();
        super.onPause();
    }
}