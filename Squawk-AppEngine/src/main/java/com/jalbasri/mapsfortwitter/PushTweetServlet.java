package com.jalbasri.mapsfortwitter;

import com.google.android.gcm.server.MulticastResult;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.URLEncoder;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.dns.HostAddress;



/**
 * Created by jalal on 10/09/13.
 */
public class PushTweetServlet extends HttpServlet {

    private static final Logger logger =
            Logger.getLogger(PushTweetServlet.class.getSimpleName());

    private static final String API_KEY = "AIzaSyC774cWSJVFtthMFQidAh1KtuHo9cuY5kM";
    
    //SENDER_ID is just the project number (see GCMIntentService)
    protected static final long SENDER_ID = 698870560273L;

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
//            String s;
//            while ((s=bufferedReader.readLine())!=null)
//            {
//                logger.info(s);
//            }
            Gson gson = new Gson();
            TweetBean tweetBean = gson.fromJson(bufferedReader, TweetBean.class);
            logger.info(tweetBean.toString());
            Sender sender = new Sender(API_KEY);
            List<String> devices = Arrays.asList(tweetBean.deviceIds);
//            doSendTweetViaGcm(tweetBean, sender, devices);
            doSendTweetViaCcs(tweetBean, devices);
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

        logger.info(String.valueOf(devices.size()));

        for (int i = 0; i < devices.size(); i++) {
            logger.info("device " + i + ": " + devices.get(i));
        }

        Message msg = new Message.Builder()
                .addData("tweet", "true")
                .addData("id", tweet.getId())
                .addData("text", URLEncoder.encode(tweet.getText(), "UTF-8"))
                .addData("created_at", tweet.getCreated_at())
                .addData("user_id", tweet.getUser_id())
                .addData("user_name", URLEncoder.encode(tweet.getUser_name(), "UTF-8"))
                .addData("user_url", tweet.getUser_url())
                .addData("screen_name", tweet.getScreen_name())
                .addData("user_image", tweet.getUser_image())
                .addData("latitude", tweet.getLatitude())
                .addData("longitude", tweet.getLongitude())
                .build();
        logger.info(msg.toString());

        MulticastResult result = sender.send(msg, devices, 5);
        logger.info("MulticastResult: " + result);

        return result;
    }

    private static boolean doSendTweetViaCcs(TweetBean tweet, List<String> devices) {

        try {
            SmackCcsClient ccsClient = new SmackCcsClient();

            logger.info("Connect with credentials, SENDER_ID: " + SENDER_ID + ", API_KEY: " + API_KEY);

            ccsClient.connect(SENDER_ID, API_KEY);

            logger.info("Connected!");

            // Send a sample hello downstream message to a device.
            logger.info("device id: " + devices.get(0));
            String toRegId = devices.get(0);
            String messageId = ccsClient.nextMessageId();
            Map<String, String> payload = new HashMap<String, String>();

            payload.put("Hello", "World");
            payload.put("CCS", "Dummy Message");
            payload.put("EmbeddedMessageId", messageId);

//            payload.put("tweet", "true");
//            payload.put("id", tweet.getId());
//            payload.put("text", URLEncoder.encode(tweet.getText(), "UTF-8"));
//            payload.put("created_at", tweet.getCreated_at());
//            payload.put("user_id", tweet.getUser_id());
//            payload.put("user_name", URLEncoder.encode(tweet.getUser_name(), "UTF-8"));
//            payload.put("user_url", tweet.getUser_url());
//            payload.put("screen_name", tweet.getScreen_name());
//            payload.put("user_image", tweet.getUser_image());
//            payload.put("latitude", tweet.getLatitude());
//            payload.put("longitude", tweet.getLongitude());


            String collapseKey = "sample";
            Long timeToLive = 10000L;
            String message = ccsClient.createJsonMessage(toRegId, messageId, payload,
                    collapseKey, timeToLive, true);
            logger.info("Created Json Message: " + message);

            boolean result = ccsClient.sendDownstreamMessage(message);
            logger.info("CCSResult: " + result);

            return result;
        }
        catch (SmackException.NotConnectedException e) {
            logger.info("NotConnectedException: " + e.getMessage());
            return false;
        }
        catch (IOException e) {
            logger.info("IOException: " + e.getMessage());
            return false;
        }
        catch (XMPPException e) {
            logger.info("XMPPException: " + e.getMessage());
            return false;
        }
        catch (SmackException e) {
            List<HostAddress> hostAddresses = ((SmackException.ConnectionException)e).getFailedAddresses();

            if (!hostAddresses.isEmpty()) {
                logger.info("HostAddress: " + hostAddresses.get(0));
                logger.info("HostAddress, Exception: " + hostAddresses.get(0).getException());
                logger.info("HostAddress, error message: " + hostAddresses.get(0).getErrorMessage());

            }

            return false;
        }
        catch (Exception e) {
            logger.info("Exception: " + e.getMessage());
            return false;
        }

    }

}