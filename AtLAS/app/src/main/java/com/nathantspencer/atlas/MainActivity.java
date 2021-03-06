package com.nathantspencer.atlas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.nathantspencer.atlas.LoginActivity.mGeneralRequest;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton mSignOutButton;
    private View mAddFriendButton;
    private View mMapView;
    private ListView mFriendsList;

    private ArrayList<String> mFriendUsernames;
    private ArrayList<Boolean> mFriendIsPending;
    private ArrayList<String> mFriendNames;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            // clear all elements before displaying those which are relevant
            mAddFriendButton.setVisibility(View.GONE);
            mMapView.setVisibility(View.GONE);
            mFriendsList.setVisibility(View.GONE);

            switch (item.getItemId()) {

                case R.id.navigation_history:
                    return true;

                case R.id.navigation_map:
                    mMapView.setVisibility(View.VISIBLE);
                    return true;

                case R.id.navigation_send:
                    mAddFriendButton.setVisibility(View.VISIBLE);
                    mFriendsList.setVisibility(View.VISIBLE);
                    return true;

                default:
                    return false;
            }
        }

    };

    private class PendingListRequestResponder implements RequestResponder {

        PendingListRequestResponder()
        {
        }

        public void onResponse(String response)
        {
            // grab value of response field "success"
            Boolean success = false;
            try
            {
                JSONObject jsonResponse = new JSONObject(response);
                success = jsonResponse.getBoolean("success");

                if(success)
                {
                    JSONArray friends = jsonResponse.getJSONArray("pending_friends");
                    for (int i = 0; i < friends.length(); i++)
                    {
                        JSONObject friend = friends.getJSONObject(i);
                        mFriendUsernames.add(friend.get("username").toString());
                        mFriendNames.add(friend.get("first_name") + " " + friend.get("last_name"));
                        mFriendIsPending.add(true);
                    }
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class FriendsListRequestResponder implements RequestResponder {

        FriendsListRequestResponder()
        {
        }

        public void onResponse(String response)
        {
            // grab value of response field "success"
            Boolean success = false;
            try
            {
                JSONObject jsonResponse = new JSONObject(response);
                success = jsonResponse.getBoolean("success");

                if(success)
                {
                    JSONArray friends = jsonResponse.getJSONArray("connections");
                    for (int i = 0; i < friends.length(); i++)
                    {
                        JSONObject friend = friends.getJSONObject(i);
                        if (friend.get("status").equals("friend"))
                        {
                            mFriendUsernames.add(friend.get("username").toString());
                            mFriendNames.add(friend.get("first_name") + " " + friend.get("last_name"));
                            mFriendIsPending.add(false);
                        }
                    }

                    final FriendsArrayAdapter arrayAdapter = new FriendsArrayAdapter
                            (mFriendUsernames, mFriendIsPending, mFriendNames, MainActivity.this);

                    mFriendsList.setAdapter(arrayAdapter);
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class LogoutRequestResponder implements RequestResponder {

        LogoutRequestResponder()
        {
        }

        public void onResponse(String response)
        {
            // grab value of response field "success"
            Boolean success = false;
            try
            {
                JSONObject jsonResponse = new JSONObject(response);
                success = jsonResponse.getBoolean("success");
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            if(success)
            {
                // remove authentication key
                SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove("atlasLoginKey");
                editor.apply();

                // move back to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
            else
            {
                // show login failure alert
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Operation failed. Please contact support!")
                        .setTitle("Logout Failed");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    protected void RefreshFriends()
    {
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        final String username = sharedPref.getString("atlasUsername", "");
        final String atlasLoginKey = sharedPref.getString("atlasLoginKey", "");

        mFriendIsPending.clear();
        mFriendUsernames.clear();
        mFriendNames.clear();

        Map<String, String> parameterBody = new HashMap<>();
        parameterBody.put("username", username);
        parameterBody.put("token", atlasLoginKey);
        mGeneralRequest.GETRequest("PendingFriends.php", parameterBody, new PendingListRequestResponder());
        mGeneralRequest.GETRequest("FriendsList.php", parameterBody, new FriendsListRequestResponder());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSignOutButton = (FloatingActionButton) findViewById(R.id.signOutButton);
        mAddFriendButton = findViewById(R.id.add_friend_button);
        mMapView = findViewById(R.id.map_view);
        mFriendsList = (ListView) findViewById(R.id.friend_list);

        mFriendUsernames = new ArrayList<>();
        mFriendIsPending = new ArrayList<>();
        mFriendNames = new ArrayList<>();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.getMenu().getItem(1).setChecked(true);

        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        final String username = sharedPref.getString("atlasUsername", "");
        final String atlasLoginKey = sharedPref.getString("atlasLoginKey", "");
        TextView usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        usernameTextView.setText(username);

        Map<String, String> parameterBody = new HashMap<>();
        parameterBody.put("username", username);
        parameterBody.put("token", atlasLoginKey);
        mGeneralRequest.GETRequest("PendingFriends.php", parameterBody, new PendingListRequestResponder());
        mGeneralRequest.GETRequest("FriendsList.php", parameterBody, new FriendsListRequestResponder());

        mSignOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, String> parameterBody = new HashMap<>();
                parameterBody.put("username", username);
                parameterBody.put("key", atlasLoginKey);
                mGeneralRequest.POSTRequest("LogUserOut.php", parameterBody, new LogoutRequestResponder());

            }
        });

        mAddFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddFriendActivity.class);
                startActivity(intent);
            }
        });

    }

}
