package com.jalbasri.squawk;

import com.google.android.gcm.server.MulticastResult;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
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

        try {
            BufferedReader bufferedReader = req.getReader();
            Gson gson = new Gson();
            TweetBean tweetBean = gson.fromJson(bufferedReader, TweetBean.class);
            System.out.println(tweetBean.toString());
            Sender sender = new Sender(API_KEY);
            List<String> devices = Arrays.asList(tweetBean.deviceIds);
            doSendTweetViaGcm(tweetBean, sender, devices);

//            for(String deviceId: tweetBean.deviceIds) {
//                doSendTweetViaGcm(tweetBean, sender, deviceId);
//            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException de-serializing POST data");
        }

    }

    /**
     * Sends the message using the Sender object to the registered device.
     *
     * @param tweet
     *            the tweet to be sent in the GCM ping to the device.
     * @param devices
     *            the registration ids of the devices.
     * @return Result the result of the ping.
     */
    private static MulticastResult doSendTweetViaGcm(TweetBean tweet, Sender sender, List<String> devices) throws IOException {

        System.out.println(devices.toString());

        Message msg = new Message.Builder()
                .addData("tweet", "true")
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

        MulticastResult result = sender.send(msg, devices, 5);
        System.out.println("Result: " + result);

        return result;
    }

}