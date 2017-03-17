package me.nwtsai.campuscow;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.Vector;
import java.util.Arrays;
import android.app.Dialog;
import android.util.Log;
import android.content.Intent;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class homescreen extends AppCompatActivity
{
    private String[] arr = new String[]{};
    private Vector<String> items = new Vector<String>(Arrays.asList(arr));
    private int playerIndex = -1;
    private int totalNumCowboys = 0;
    private int totalNumCows = 0;
    private int canAddCow = -1;
    private static final int LOCATION_REQUEST = 0;
    private boolean STARTGAMEPRESSED = false;

    // API Calls
    private String BASE_URL = "http://campuscow.us-east-1.elasticbeanstalk.com/";
    private String GET_ID = "getid";
    private String TOTAL_IDS = "totalids";
    private String CREATE_COW = "createcow";

    // For running a method every TIME_INTERVAL ms
    private Handler handlerRefreshLobby = new Handler();
    private Handler handlerRequestAddCowBoy = new Handler();
    private Handler handlerRequestAddCow = new Handler();
    private final int TIME_INTERVAL = 1 * 1000;

    // When the initial screen loads, call this function
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Log.e("Location err", "permission failed");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }

        // Reference the objects on the screen
        ListView listView = (ListView)findViewById(R.id.myList);

        // Adapter that manages the listView
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(homescreen.this,android.R.layout.simple_list_item_1, this.items);
        listView.setAdapter(adapter);

        // Start game button
        final FloatingActionButton startGame = (FloatingActionButton) findViewById(R.id.startGame);
        startGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                STARTGAMEPRESSED = true;
                transitionToNewActivity(getPlayerIndex());
            }
        });

        // Hide the play game button initially
        startGame.setVisibility(View.GONE);

        // Join game button
        final FloatingActionButton joinGame = (FloatingActionButton) findViewById(R.id.joinGame);
        joinGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                askForPlayerType(adapter, startGame, joinGame);
            }
        });

        // Continually add users to the list
        requestRefreshLobby(adapter);
    }

    // Run this method every TIME_INTERVAL seconds
    public void requestRefreshLobby(final ArrayAdapter<String> adapter)
    {
        handlerRefreshLobby.postDelayed(new Runnable()
        {
            public void run()
            {

                refreshLobby(adapter);

                // Stop callbacks once STARTGAMEPRESSED is true
                if (STARTGAMEPRESSED == true)
                {
                    handlerRefreshLobby.removeCallbacksAndMessages(null);
                }
                else
                {
                    handlerRefreshLobby.postDelayed(this, TIME_INTERVAL);
                }
            }
        }, TIME_INTERVAL);
    }

    // Continually add users to the list
    public void refreshLobby(final ArrayAdapter<String> adapter)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, BASE_URL + TOTAL_IDS, null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response) {
                try
                {
                    Integer cow = response.getInt("cow");
                    Integer num = response.getInt("num");

                    // If I tried adding a cow and I'm not the cow
                    if (playerIndex != 0 && cow == 1 && totalNumCows == 0)
                    {
                        totalNumCows++;
                        addItem(adapter, "Campus Cow");
                    }

                    // If the number cowboys fetched exceeds the current list, add items to match the number
                    while (totalNumCowboys < num)
                    {
                        totalNumCowboys++;
                        addItem(adapter, "Cowboy " + Integer.toString(totalNumCowboys));
                    }

                    Log.e("refreshLobby", "hasRun");
                }
                catch (JSONException e)
                {
                    Log.e("Can't refreshLobby", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TO DO Auto generated method stub
                Log.e("Volley ID Error", error.toString());
            }
        });

        queue.add(jsObjRequest);
    }

    // Create a dropdown dialog asking for the player's preferred player type
    public void askForPlayerType(final ArrayAdapter<String> adapter, final FloatingActionButton startGame, final FloatingActionButton joinGame)
    {
        String[] playerTypeArray = new String[]{"COW", "COWBOY"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("JOIN GAME AS:").setItems(playerTypeArray, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {

                // The 'which' argument contains the index position of the selected item
                if (which == 0)
                {
                    // Request until cow can be added gets a response
                    requestCanAddCow(adapter, joinGame, startGame);
                }
                else
                {
                    // Request Cowboy ID until we get a response
                    requestCowboyID(adapter, startGame);
                }

                // Set the join game to be invisible and the start game button to be visible
                joinGame.setVisibility(View.GONE);
            }
        });

        // Create and show the dialog
        Dialog dialog = builder.create();
        dialog.show();
    }

    // Run this method every TIME_INTERVAL seconds
    public void requestCowboyID(final ArrayAdapter<String> adapter, final FloatingActionButton startGame)
    {
        handlerRequestAddCowBoy.postDelayed(new Runnable()
        {
            public void run()
            {

                getCowboyID(adapter, startGame);

                // Call this method again if we haven't added ourselves to the list
                if (playerIndex != -1)
                {
                    handlerRequestAddCowBoy.postDelayed(this, TIME_INTERVAL);
                }

                // If we added ourselves to the list, stop callbacks
                else
                {
                    handlerRequestAddCowBoy.removeCallbacksAndMessages(null);
                }
            }
        }, TIME_INTERVAL);
    }

    // Get a valid User ID
    public void getCowboyID(final ArrayAdapter<String> adapter, final FloatingActionButton startGame)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, BASE_URL + GET_ID, null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response) {
                try
                {
                    playerIndex = response.getInt("id");
                    addItem(adapter, "Cowboy " + Integer.toString(playerIndex) + " (You)");
                    totalNumCowboys++;
                    startGame.setVisibility(View.VISIBLE);
                    Log.e("getCowboyID", "hasRun");
                }
                catch (JSONException e)
                {
                    Log.e("Can't getCowboyID", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TO DO Auto generated method stub
                Log.e("Volley ID Error", error.toString());
            }
        });

        queue.add(jsObjRequest);
    }

    // Run this method every TIME_INTERVAL seconds
    public void requestCanAddCow(final ArrayAdapter<String> adapter, final FloatingActionButton joinGame, final FloatingActionButton startGame)
    {
        handlerRequestAddCow.postDelayed(new Runnable()
        {
            public void run()
            {

                getCanAddCow(adapter, startGame);

                // If a cow already exists and it isn't us
                if (canAddCow == 1)
                {
                    displayCannotBeCowAlert(joinGame);
                    handlerRequestAddCow.removeCallbacksAndMessages(null);
                }
                // If we successful can add a cow as ourselves, remove callbacks
                else if (canAddCow == 0)
                {
                    handlerRequestAddCow.removeCallbacksAndMessages(null);
                }

                // Otherwise keep trying
                else
                {
                    handlerRequestAddCow.postDelayed(this, TIME_INTERVAL);
                }

            }
        }, TIME_INTERVAL);
    }

    // See if we can add a cow
    public void getCanAddCow(final ArrayAdapter<String> adapter, final FloatingActionButton startGame)
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, BASE_URL + CREATE_COW, null, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response) {
                try
                {
                    // 0 means can add cow, 1 means we can't add cow
                    canAddCow = response.getInt("cow");

                    // If we can add a cow, add a cow
                    if (canAddCow == 0)
                    {
                        addItem(adapter, "Campus Cow (You)");
                        totalNumCows++;
                        playerIndex = 0;
                        startGame.setVisibility(View.VISIBLE);
                    }

                    Log.e("getCanAddCow", "hasRun");
                }
                catch (JSONException e)
                {
                    Log.e("Can't getCanAddCow", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //TO DO Auto generated method stub
                Log.e("Volley Cow Error", error.toString());
            }
        });

        queue.add(jsObjRequest);
    }

    // Tell the user that the Campus Cow is already taken, and that they must choose another player type
    public void displayCannotBeCowAlert(final FloatingActionButton joinGame)
    {
        // Stop the recurring function
        handlerRequestAddCow.removeCallbacksAndMessages(null);

        // Create a non-cancelable dialog for the winner
        android.app.AlertDialog.Builder alertBuilder = new android.app.AlertDialog.Builder(homescreen.this);
        alertBuilder.setTitle("A cow already exists; please choose another player type.");
        alertBuilder.setCancelable(false).setPositiveButton("Okay", new DialogInterface.OnClickListener()
        {
            // When the OKAY button is pressed, dismiss dialog and make the Join Game Button visible again
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                joinGame.setVisibility(View.VISIBLE);
            }
        });

        // Create and show the dialog
        Dialog dialog = alertBuilder.create();
        dialog.show();
    }

    // Public getter function for getting the player index
    public int getPlayerIndex()
    {
        return this.playerIndex;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case LOCATION_REQUEST:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {}
                else {}
                return;
            }
        }
    }

    // Transition to the map once the play button is pressed
    void transitionToNewActivity(int pI)
    {
        Intent myIntent = new Intent(homescreen.this, map.class);
        myIntent.putExtra("playerIndex", pI);
        homescreen.this.startActivity(myIntent);
    }

    // Pass in the adapter as an argument
    void addItem(ArrayAdapter<String> adp, String input)
    {
        this.items.addElement(input);
        adp.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_homescreen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
