// Utils.java --
//
// Utils.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.jboss.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;

import com.electriccloud.commander.gwt.client.BrowserContext;
import com.electriccloud.commander.gwt.client.requests.CgiRequestProxy;

import static com.electriccloud.commander.gwt.client.ComponentBaseFactory.getPluginName;

public class JobUtilities
{

    //~ Methods ----------------------------------------------------------------

    public static void waitForJob(
            final String    jobId,
            RequestCallback callback)
        throws RequestException
    {
        CgiRequestProxy     cgiRequestProxy = new CgiRequestProxy(
                getPluginName(), "jbossMonitor.cgi");
        Map<String, String> cgiParams       = new HashMap<String, String>();

        cgiParams.put("jobId", jobId);

        // Pass debug flag to CGI, which will use it to determine whether to
        // clean up a successful job
        if ("1".equals(BrowserContext.getInstance()
                                     .getGetParameter("debug"))) {
            cgiParams.put("debug", "1");
        }

        cgiRequestProxy.issueGetRequest(cgiParams, callback);
    }
}
