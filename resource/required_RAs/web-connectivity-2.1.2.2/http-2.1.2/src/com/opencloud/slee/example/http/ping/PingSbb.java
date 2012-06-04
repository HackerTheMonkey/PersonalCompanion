package com.opencloud.slee.example.http.ping;

import com.opencloud.slee.resources.http.HttpRequest;
import com.opencloud.slee.resources.http.HttpResponse;
import com.opencloud.slee.resources.http.IncomingHttpRequestActivity;
import com.opencloud.slee.services.common.BaseSbb;

import javax.slee.ActivityContextInterface;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;

import java.util.Iterator;

/**
 * Simple HTTP "Ping" SBB - just returns an OK response
 */ 
public abstract class PingSbb extends BaseSbb {

    public void setSbbContext(SbbContext sbbContext) {
        super.setSbbContext(sbbContext);
    }

    protected String getTraceType() { return "Ping"; }

    /**
     * Handles HTTP GET request
     */
    public void onGetRequest(HttpRequest request, ActivityContextInterface aci) {
        if (isFinestTraceable()) finest("onGetRequest: received request: " + request);
        try {
            IncomingHttpRequestActivity activity = 
                    (IncomingHttpRequestActivity) aci.getActivity();

            // create the OK response object
            HttpResponse response = activity.createResponse(200, "OK");

            // set some test content
            StringBuffer content = new StringBuffer(header);
            content.append("<h1>Ping SBB</h1>\n");
            content.append("<p>SbbID: ").append(getSbbContext().getSbb());
            content.append("<p>HTTP version: ").append(request.getVersion().toString());
            content.append("<p>URL: ").append(request.getRequestURL());
            content.append("<p>Request headers:\n<pre>");
            for (Iterator i = request.getHeaderNames(); i.hasNext(); ) {
                String headerName = (String)i.next();
                content.append(headerName + ": " + request.getHeader(headerName) + "\n");
            }
            content.append("</pre>\n");
            content.append("<p>Request content length: " + request.getContentLength() + " bytes\n<pre>");
            if (request.getContentLength() > 0)
                content.append("<p>Raw content:\n<pre>").append(request.getContentAsString()).append("</pre>\n");
            content.append(footer);
            response.setContentAsString("text/html; charset=\"utf-8\"", content.toString());

            // send the response
            if (isFinestTraceable()) finest("onGetRequest: sending response: " + response);
            activity.sendResponse(response);
            
        } catch (Exception e) {
            warning("unable to generate response", e);
        }
    }

    public void sbbRolledBack(RolledBackContext context) {}
    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface aci) {}
    
    public static final String header =
            "<html><head><title>HTTP Ping SBB</title></head><body>\n";
    public static final String footer =
            "</body></html>";
}

