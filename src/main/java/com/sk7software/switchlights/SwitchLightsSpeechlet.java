/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sk7software.switchlights;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;

import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazonaws.util.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Andrew
 */
public class SwitchLightsSpeechlet implements SpeechletV2 {
    private static final Logger log = LoggerFactory.getLogger(SwitchLightsSpeechlet.class);

    private static final String SWITCH_URL = "http://www.sk7software.co.uk/Zwave/index.php?action=";

    @Override
    public void onSessionStarted(final SpeechletRequestEnvelope<SessionStartedRequest> speechletRequestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}",
                speechletRequestEnvelope.getRequest().getRequestId(),
                speechletRequestEnvelope.getSession().getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(final SpeechletRequestEnvelope<LaunchRequest> speechletRequestEnvelope) {
        log.info("onLaunch requestId={}, sessionId={}",
                speechletRequestEnvelope.getRequest().getRequestId(),
                speechletRequestEnvelope.getSession().getSessionId());
        return getHelpResponse();
    }

    @Override
    public SpeechletResponse onIntent(final SpeechletRequestEnvelope<IntentRequest> speechletRequestEnvelope) {
        IntentRequest request = speechletRequestEnvelope.getRequest();
        Session session = speechletRequestEnvelope.getSession();
        log.info("onIntent requestId={}, sessionId={}, intentName={}", request.getRequestId(),
                session.getSessionId(), (request.getIntent() != null ? request.getIntent().getName() : "null"));

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : "Invalid";

        switch (intentName) {
            case "SwitchIntent":
                return getSwitchResponse(intent.getSlot("onoff").getValue());
            case "AMAZON.HelpIntent":
                return getHelpResponse();
            default:
                return null; // TODO: return error
        }
    }

    @Override
    public void onSessionEnded(final SpeechletRequestEnvelope<SessionEndedRequest> speechletRequestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}",
                speechletRequestEnvelope.getRequest().getRequestId(),
                speechletRequestEnvelope.getSession().getSessionId());
    }


    private SpeechletResponse getSwitchResponse(String onoff) {
        StringBuilder speechText = new StringBuilder();

        if (!"on".equals(onoff) && !"off".equals(onoff)) {
            speechText.append("Please say 'switch on', or 'switch off'");
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(speechText.toString());
            Reprompt reprompt = new Reprompt();
            PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
            repromptSpeech.setText("");
            reprompt.setOutputSpeech(repromptSpeech);
            return SpeechletResponse.newAskResponse(speech, reprompt);
        } else {
            try {
                String url = SWITCH_URL + onoff;
                String response = getJsonResponse(url);
                speechText.append("Lights ");
                speechText.append(onoff);
            } catch (Exception e) {
                speechText.append("Sorry, there was a problem with that command");
                log.error(e.getMessage());
            }

            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            speech.setText(speechText.toString());
            return SpeechletResponse.newTellResponse(speech);
        }
    }


    public static String getJsonResponse(String requestURL) {
        InputStreamReader inputStream = null;
        BufferedReader bufferedReader = null;
        String text;
        try {
            String line;
            URL url = new URL(requestURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            // set up url connection to get retrieve information back
            con.setRequestMethod("GET");
            con.setReadTimeout(20000);

            inputStream = new InputStreamReader(con.getInputStream(), Charset.forName("US-ASCII"));
            bufferedReader = new BufferedReader(inputStream);
            StringBuilder builder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            text = builder.toString();
        } catch (IOException e) {
            // reset text variable to a blank string
            log.error(e.getMessage());
            text = "";
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(bufferedReader);
        }

        return text;
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        StringBuilder helpText = new StringBuilder();
        helpText.append("You can ask to switch on or off.");

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(helpText.toString());

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText("");
        reprompt.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }
}
