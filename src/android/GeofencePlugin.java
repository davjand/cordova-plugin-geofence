package com.cowbell.cordova.geofence;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.Manifest;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    private GeoNotificationManager geoNotificationManager;
    private Context context;
    protected static Boolean isInBackground = true;
    public static CordovaWebView webView = null;

    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }

    //FIXME: what about many executedActions at once
    private Action executedAction;

    /**
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        context = this.cordova.getActivity().getApplicationContext();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
    }

    @Override
    public boolean execute(String action, JSONArray args,
                           CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeofencePlugin execute action: " + action + " args: "
                + args.toString());
        executedAction = new Action(action, args, callbackContext);

        if (action.equals("addOrUpdate")) {
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for (int i = 0; i < args.length(); i++) {
                GeoNotification not = parseFromJSONObject(args.getJSONObject(i));
                if (not != null) {
                    geoNotifications.add(not);
                }
            }
            geoNotificationManager.addGeoNotifications(geoNotifications,
                    callbackContext);
        } else if (action.equals("remove")) {
            List<String> ids = new ArrayList<String>();
            for (int i = 0; i < args.length(); i++) {
                ids.add(args.getString(i));
            }
            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        } else if (action.equals("removeAll")) {
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
        } else if (action.equals("getWatched")) {
            List<GeoNotification> geoNotifications = geoNotificationManager
                    .getWatched();
            callbackContext.success(Gson.get().toJson(geoNotifications));
        } else if (action.equals("initialize")) {
            initialize(callbackContext);
        } else if (action.equals("deviceReady")) {
            deviceReady();
        } else if (action.equals("clearCheckinCache")) {
            clearCheckinCache();
        } else if (action.equals("__test")) {
            __test();
        } else {
            return false;
        }

        return true;
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public static void onTransitionReceived(List<GeoNotification> notifications) {
        Log.d(TAG, "Transition Event Received!");
        String js = "setTimeout('geofence.onTransitionReceived("
                + Gson.get().toJson(notifications) + ")',0)";
        if (webView == null) {
            handleGeofenceInBackground(notifications);
        } else {
            webView.sendJavascript(js);
        }
    }

    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        String data = intent.getStringExtra("geofence.notification.data");
        String js = "setTimeout('geofence.onNotificationClicked("
                + data + ")',0)";

        if (data == null) {
            Log.d(TAG, "No notifications clicked.");
        } else {
            webView.sendJavascript(js);
        }

        checkCheckinCacheAfterLaunch();
    }


    private void initialize(CallbackContext callbackContext) throws JSONException {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(permissions)) {
            PermissionHelper.requestPermissions(this, 0, permissions);
        } else {
            callbackContext.success();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasPermission(this, permission)) {
                return false;
            }
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;

        if (executedAction != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    executedAction.callbackContext.sendPluginResult(result);
                    executedAction = null;
                    return;
                }
            }
            Log.d(TAG, "Permission Granted!");

            //try to execute method once again
            execute(executedAction);
            executedAction = null;
        }
    }





    static int _callCount = 0;
    static String _cacheFileName = "checkin_cache.json";

    public void __test() {

        // This method gets called twice when on debug mode ?

        _callCount++;

        if (_callCount % 2 == 0) {
            return;
        }

        /**
         *
         *
         * CODE HERE
         *
         *
         */


        // TransitionType.Enter = 1
        // TransitionType.Exit  = 2
        // TransitionType.Both  = 3


        // Create a test notification
        GeoNotification notif = new GeoNotification();
        notif.id = "TestId";
        notif.latitude = 54.999489;
        notif.longitude = -1.6050274;
        notif.transitionType = 1;


        // Create an array of the notifications
        List<GeoNotification> dummyNotifs = new ArrayList<GeoNotification>();
        dummyNotifs.add(notif);


        // Call our function to test it
        handleGeofenceInBackground(dummyNotifs);



    }

    /** Called when the app was launched in the background for a geofence check in */
    private static void handleGeofenceInBackground(List<GeoNotification> notifications) {

        // Check if there is already a cache
        JSONArray cache = null;
        String existingCache = readFile(_cacheFileName);


        // If there is a cache, load the data into a JSONArray
        if (existingCache.equals("") == false) {

            try {
                cache = new JSONArray(existingCache);
            }
            catch (JSONException e) {

                // Error: cache was unreadable!
            }

        }


        // If the cache couldn't be found, create a new one
        if (cache  == null) {
            cache = new JSONArray();
        }


        // Loop the geofence notifications
        for (GeoNotification notif : notifications) {

            try {

                // Create a json object from the notification
                JSONObject jsonObj = new JSONObject(Gson.get().toJson(notif));

                // Put in the current timestamp (formatted correctly?)
                jsonObj.put("timestamp", new Date());

                // Add each notification to the array
                cache.put(jsonObj);
            }
            catch (Exception e) {

                // Error: Couldn't write geofence notification
            }
        }


        // Write the cache to file
        if (writeToFile(cache.toString(), _cacheFileName) == false) {

            // Error: Couldn't write cache
        }
    }

    /** Called after the app launched to pass any cached geofence check ins to the js app */
    private void checkCheckinCacheAfterLaunch() {

        // Try to load the cache
        String cache = readFile(_cacheFileName);


        // If it wasn't loaded, do nothing
        if (cache.equals("")) { return; }


        // Make the string into a method call
        String js = "setTimeout('geofence.onNotificationClicked("
                + cache + ")',0)";


        // Pass that to the webView
        webView.sendJavascript(js);
    }

    /** Called by the js app when it has receieved the cached geofence check ins
        In a seperate function so the cache is ONLY removed once they've been processed */
    private void clearCheckinCache() {

        // Check if there are any check-ins cached
        String cache = readFile(_cacheFileName);

        // Remove them
    }

    /** A utility function to write the cache to a file */
    private static boolean writeToFile(String data, String filename) {

        // If there is no web view, we can't get a Context
        if (webView == null) { return false; }

        // Get the context from the webView
        Context context = webView.getContext();

        try {

            // Create a stream and write the data to it
            OutputStreamWriter writer = new OutputStreamWriter( context.openFileOutput(filename, Context.MODE_PRIVATE));
            writer.write(data);
            writer.close();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /** A utility function to load in a string */
    private static String readFile(String filename) {

        // If there is no web view, we can't get a Context
        if (webView == null) { return ""; }

        try {

            // Create a stream to read in from
            InputStream stream = webView.getContext().openFileInput(filename);

            // Check the stream was created
            if (stream != null) {

                // Build a string from the read in data
                InputStreamReader reader = new InputStreamReader(stream);
                StringBuilder output = new StringBuilder();
                BufferedReader buffer = new BufferedReader(reader);
                String nextLine = "";

                while ((nextLine = buffer.readLine()) != null) {
                    output.append(nextLine);
                }

                // Return the read-in string
                return output.toString();
            }
        }
        catch (Exception e) {
            // Catch errors ...
        }

        return "";
    }

}
