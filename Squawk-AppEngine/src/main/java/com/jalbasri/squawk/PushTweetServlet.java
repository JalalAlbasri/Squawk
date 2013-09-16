package com.jalbasri.squawk;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

/**
 * Created by jalal on 10/09/13.
 */
public class PushTweetServlet extends HttpServlet {

    private static final Logger logger =
            Logger.getLogger(PushTweetServlet.class.getSimpleName());

    private static final String API_KEY = "AIzaSyCvAjdKINk6Zc72_M5tIRRneTsUEbZFgm8";

    private static final DeviceInfoEndpoint endpoint = new DeviceInfoEndpoint();

    public void init(ServletConfig config) throws ServletException {
        super.init(config);


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        pushTweet(req, resp);
    }

    private void pushTweet(HttpServletRequest req, HttpServletResponse resp) {
        //TODO extract tweet info from json, push to message queue.


        try {
//            Spit out the raw request body.. testing..
//            StringBuilder buffer = new StringBuilder();
//            BufferedReader reader = req.getReader();
//
//            String line;
//            while((line = reader.readLine()) != null){
//                buffer.append(line);
//            }
//            // reqBytes = buffer.toString().getBytes();
//
//            String input = buffer.toString();
//            System.out.println(input);



            BufferedReader bufferedReader = req.getReader();
            Gson gson = new Gson();
            TweetBean tweetBean = gson.fromJson(bufferedReader, TweetBean.class);
            System.out.println(tweetBean.toString());

            Sender sender = new Sender(API_KEY);
//            doSendTweetViaGcm(tweetBean, sender, tweetBean.getDeviceId());

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException de-serializing POST data");
        }

    }

    /**
     * Sends the message using the Sender object to the registered device.
     *
     * @param tweet
     *            the tweet to be sent in the GCM ping to the device.
     * @param sender
     *            the Sender object to be used for ping,
     * @param deviceId
     *            the registration id of the device.
     * @return Result the result of the ping.
     */
    private static Result doSendTweetViaGcm(TweetBean tweet, Sender sender,
                                       String deviceId) throws IOException {

        DeviceInfo deviceInfo = endpoint.getDeviceInfo(deviceId);

        Message msg = new Message.Builder().addData("tweet", "true")
                .addData("id", tweet.getId())
                .addData("text", tweet.getText())
                .addData("created_at", tweet.getCreated_at())
                .addData("user_id", tweet.getUser_id())
                .addData("user_name", tweet.getUser_name())
                .addData("user_url", tweet.getUser_url())
                .addData("screen_name", tweet.getScreen_name())
                .addData("user_image", tweet.getUser_image())
                .addData("latitude", tweet.getLatitude())
                .addData("longitude", tweet.getLongitude())
                .build();

        Result result = sender.send(msg, deviceId, 5);

        if (result.getMessageId() != null) {
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                endpoint.removeDeviceInfo(deviceId);
                deviceInfo.setDeviceRegistrationID(canonicalRegId);
                endpoint.insertDeviceInfo(deviceInfo);
            }
        } else {
            String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                //If the returned error is NotRegistered, it's necessary to remove that registration ID,
                //because the application was uninstalled from the device.
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
            }
        }

        return result;
    }

}
