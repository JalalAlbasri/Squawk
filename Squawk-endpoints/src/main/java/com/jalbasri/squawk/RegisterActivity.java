package com.jalbasri.squawk;

import java.io.IOException;

import com.jalbasri.squawk.messageEndpoint.MessageEndpoint;
import com.jalbasri.squawk.messageEndpoint.model.CollectionResponseMessageData;
import com.jalbasri.squawk.messageEndpoint.model.MessageData;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnTouchListener;
import android.widget.TextView;


/**
 * An activity that communicates with your App Engine backend via Cloud
 * Endpoints.
 *
 * When the user hits the "Register" button, a message is sent to the backend
 * (over endpoints) indicating that the device would like to receive broadcast
 * messages from it. Clicking "Register" also has the effect of registering this
 * device for Google Cloud Messaging (GCM). Whenever the backend wants to
 * broadcast a message, it does it via GCM, so that the device does not need to
 * keep polling the backend for messages.
 *
 * If you've generated an App Engine backend for an existing Android project,
 * this activity will not be hooked in to your main activity as yet. You can
 * easily do so by adding the following lines to your main activity:
 *
 * Intent intent = new Intent(this, RegisterActivity.class);
 * startActivity(intent);
 *
 * To make the sample run, you need to set your PROJECT_NUMBER in
 * GCMIntentService.java. If you're going to be running a local version of the
 * App Engine backend (using the DevAppServer), you'll need to toggle the
 * LOCAL_ANDROID_RUN flag in CloudEndpointUtils.java. See the javadoc in these
 * classes for more details.
 *
 * For a comprehensive walkthrough, check out the documentation at
 * http://developers.google.com/eclipse/docs/cloud_endpoints
 */
public class RegisterActivity extends Activity {

    private static final String TAG = RegisterActivity.class.getSimpleName();

    enum State {
        REGISTERED, REGISTERING, UNREGISTERED, UNREGISTERING
    }

    private State curState = State.UNREGISTERED;
    private OnTouchListener registerListener = null;
    private OnTouchListener unregisterListener = null;
    private MessageEndpoint messageEndpoint = null;

    /*
    Original onCreate rewritten below to remove user interaction
    */

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_register);
//
//        Button regButton = (Button) findViewById(R.id.regButton);
//
//        registerListener = new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                    case MotionEvent.ACTION_DOWN:
//                        if (GCMIntentService.PROJECT_NUMBER == null
//                                || GCMIntentService.PROJECT_NUMBER.length() == 0) {
//                            showDialog("Unable to register for Google Cloud Messaging. "
//                                    + "Your application's PROJECT_NUMBER field is unset! You can change "
//                                    + "it in GCMIntentService.java");
//                        } else {
//                            updateState(State.REGISTERING);
//                            try {
//                                GCMIntentService.register(getApplicationContext());
//                            } catch (Exception e) {
//                                Log.e(RegisterActivity.class.getName(),
//                                        "Exception received when attempting to register for Google Cloud "
//                                                + "Messaging. Perhaps you need to set your virtual device's "
//                                                + " target to Google APIs? "
//                                                + "See https://developers.google.com/eclipse/docs/cloud_endpoints_android"
//                                                + " for more information.", e);
//                                showDialog("There was a problem when attempting to register for "
//                                        + "Google Cloud Messaging. If you're running in the emulator, "
//                                        + "is the target of your virtual device set to 'Google APIs?' "
//                                        + "See the Android log for more details.");
//                                updateState(State.UNREGISTERED);
//                            }
//                        }
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//        };
//
//        unregisterListener = new OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                    case MotionEvent.ACTION_DOWN:
//                        updateState(State.UNREGISTERING);
//                        GCMIntentService.unregister(getApplicationContext());
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//        };
//
//        regButton.setOnTouchListener(registerListener);
//
//    /*
//     * build the messaging endpoint so we can access old messages via an endpoint call
//     */
//        MessageEndpoint.Builder endpointBuilder = new MessageEndpoint.Builder(
//                AndroidHttp.newCompatibleTransport(),
//                new JacksonFactory(),
//                new HttpRequestInitializer() {
//                    public void initialize(HttpRequest httpRequest) { }
//                });
//
//        messageEndpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "REGISTER ACTIVITY ONCREATE");
        super.onCreate(savedInstanceState);
        if (GCMIntentService.PROJECT_NUMBER == null
                || GCMIntentService.PROJECT_NUMBER.length() == 0) {
            setResult(RESULT_CANCELED);
        } else {
            try {
                GCMIntentService.register(getApplicationContext());
            } catch (Exception e) {
                Log.e(RegisterActivity.class.getName(),
                        "Exception received when attempting to register for Google Cloud ", e);
                setResult(RESULT_CANCELED);
            }
        }

    /*
     * build the messaging endpoint so we can access old messages via an endpoint call
     */
        MessageEndpoint.Builder endpointBuilder = new MessageEndpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) { }
                });

        messageEndpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    /*
     * If we are dealing with an intent generated by the GCMIntentService
     * class, then display the provided message.
     */
        if (intent.getBooleanExtra("gcmIntentServiceMessage", false)) {
            Log.d(TAG, "onNewIntent: Registration Message received");
//            showDialog(intent.getStringExtra("message"));

            if (intent.getBooleanExtra("registrationMessage", false)) {

                if (intent.getBooleanExtra("error", false)) {
          /*
           * If we get a registration/unregistration-related error,
           * Return failure code
           */
                    Log.d(TAG, "onNewIntent: Registration Error, SetResult Canceled");
                    setResult(RESULT_CANCELED);

                } else {
          /*
           * If we get a registration/unregistration-related success,
           * Return success code
           */
                    Log.d(TAG, "onNewIntent: Registration Success SetResult OK");
                    setResult(RESULT_OK, intent);
                }
            }
            else {
        /* 
         * if we didn't get a registration/unregistration message then
         * go get the last 5 messages from app-engine
         */
                new QueryMessagesTask(this, messageEndpoint).execute();
            }
            finish();
        }
    }

//    private void showDialog(String message) {
//        new AlertDialog.Builder(this)
//                .setMessage(message)
//                .setPositiveButton(android.R.string.ok,
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.dismiss();
//                            }
//                        }).show();
//    }

    /*
     * Need to run this in background so we don't hold up the UI thread,
     * this task will ask the App Engine backend for the last 5 messages
     * sent to it
     */
    private class QueryMessagesTask
            extends AsyncTask<Void, Void, CollectionResponseMessageData> {
        Exception exceptionThrown = null;
        MessageEndpoint messageEndpoint;

        public QueryMessagesTask(Activity activity, MessageEndpoint messageEndpoint) {
            this.messageEndpoint = messageEndpoint;
        }

        @Override
        protected CollectionResponseMessageData doInBackground(Void... params) {
            try {
                CollectionResponseMessageData messages =
                        messageEndpoint.listMessages().setLimit(5).execute();
                return messages;
            } catch (IOException e) {
                exceptionThrown = e;
                return null;
                //Handle exception in PostExecute
            }
        }

        protected void onPostExecute(CollectionResponseMessageData messages) {
            // Check if exception was thrown
            if (exceptionThrown != null) {
                Log.e(RegisterActivity.class.getName(),
                        "Exception when listing Messages", exceptionThrown);
//                showDialog("Failed to retrieve the last 5 messages from " +
//                        "the endpoint at " + messageEndpoint.getBaseUrl() +
//                        ", check log for details");
            }
            else {
                TextView messageView = (TextView) findViewById(com.jalbasri.squawk.R.id.msgView);
                messageView.setText("Last 5 Messages read from " +
                        messageEndpoint.getBaseUrl() + ":\n");
                for(MessageData message : messages.getItems()) {
                    messageView.append(message.getMessage() + "\n");
                }
            }
        }
    }
}
