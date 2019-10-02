// EditConfigPropertySheetEditor.java --
//
// EditConfigPropertySheetEditor.java is part of ElectricCommander.
//
// Copyright (c) 2005-2017 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.jboss.client;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Anchor;

import com.electriccloud.commander.client.domain.Procedure;
import com.electriccloud.commander.client.requests.GetProcedureRequest;
import com.electriccloud.commander.client.requests.RunProcedureRequest;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.DefaultProcedureCallback;
import com.electriccloud.commander.client.responses.DefaultRunProcedureResponseCallback;
import com.electriccloud.commander.client.responses.RunProcedureResponse;
import com.electriccloud.commander.gwt.client.ui.CredentialEditor;
import com.electriccloud.commander.gwt.client.ui.FormBuilder;
import com.electriccloud.commander.gwt.client.ui.SimpleErrorBox;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;

import ecinternal.client.PropertySheetEditor;
import ecplugins.jboss.client.JobUtilities;

import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createUrl;

public class EditConfigPropertySheetEditor
    extends PropertySheetEditor
{

    //~ Instance fields --------------------------------------------------------

    private String m_pluginName;
    private String m_configName;

    //~ Constructors -----------------------------------------------------------

    public EditConfigPropertySheetEditor(
            String idPrefix,
            String mainTitle,
            String secondaryTitle,
            String propertySheetPath)
    {
        super(idPrefix, mainTitle, secondaryTitle, propertySheetPath);
    }

    public EditConfigPropertySheetEditor(
            String idPrefix,
            String mainTitle,
            String configName,
            String propertySheetPath,
            String formDefinitionXmlPath,
            String formCredentialProject,
            String pluginName)
    {
        super(idPrefix, mainTitle, configName, propertySheetPath,
            formDefinitionXmlPath, formCredentialProject);
        m_configName = configName;
        m_pluginName = pluginName;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public void clearStatus()
    {
        super.clearStatus();
        getFormTable().removeRow("config");
    }

    @Override protected void submit()
    {
        setStatus("Saving...");
        clearAllErrors();

        GetProcedureRequest getProcedureRequest = getRequestFactory()
                .createGetProcedureRequest();

        getProcedureRequest.setProjectName(m_pluginName);
        getProcedureRequest.setProcedureName("EditConfiguration");
        getProcedureRequest.setCallback(new DefaultProcedureCallback(
                getCommanderErrorHandler()) {
                @Override public void handleError(CommanderError error)
                {
                    if (error.getCode()
                             .equals("NoSuchProcedure")) {

                        // procedure doesn't exist, so, it is fine.
                        EditConfigPropertySheetEditor.super.submit();
                    }
                    else {
                        addErrorMessage(error);
                    }
                }

                @Override public void handleResponse(Procedure response)
                {
                    FormBuilder         fb               = (FormBuilder)
                        getFormTable();
                    Map<String, String> params           = fb.getValues();
                    Collection<String>  credentialParams =
                        fb.getCredentialIds();
                    RunProcedureRequest request          = getRequestFactory()
                            .createRunProcedureRequest();
                    if (!fb.validate()) {
                        clearStatus();

                        return;
                    }
                    request.setProjectName(
                        "/plugins/" + m_pluginName + "/project");
                    request.setProcedureName("EditConfiguration");
                    request.addActualParameter("config", m_configName);

                    for (String paramName : params.keySet()) {

                        if (credentialParams.contains(paramName)) {
                            CredentialEditor credential = fb.getCredential(
                                    paramName);

                            request.addCredentialParameter(paramName,
                                credential.getUsername(),
                                credential.getPassword());
                        }
                        else {
                            request.addActualParameter(paramName,
                                params.get(paramName));
                        }
                    }

                    request.setCallback(
                        new DefaultRunProcedureResponseCallback(this) {
                            @Override public void handleResponse(
                                    RunProcedureResponse response)
                            {

                                if (getLog().isDebugEnabled()) {
                                    getLog().debug(
                                        "Commander runProcedure request returned job id: "
                                            + response.getJobId());
                                }

                                waitForJob(response.getJobId());
                            }
                        });
                    doRequest(request);
                }
            });
        doRequest(getProcedureRequest);
    }

    private void waitForJob(final String jobId)
    {
        RequestCallback requestCallback = new RequestCallback() {
            @Override public void onError(
                    Request   request,
                    Throwable exception)
            {
                addErrorMessage("CGI request failed: ", exception);
            }

            @Override public void onResponseReceived(
                    Request  request,
                    Response response)
            {
                String responseString = response.getText();

                if (getLog().isDebugEnabled()) {
                    getLog().debug("CGI response received: " + responseString);
                }

                if (responseString.startsWith("Success")) {
                    EditConfigPropertySheetEditor.super.submit();
                    // super.submit();
                }
                else {
                    SimpleErrorBox      error      = getUIFactory()
                            .createSimpleErrorBox(
                                "Error occurred during config edit: "
                                + responseString);
                    CommanderUrlBuilder urlBuilder = createUrl("jobDetails.php")
                            .setParameter("jobId", jobId);

                    error.add(new Anchor("(See job for details)",
                            urlBuilder.buildString()));
                    addErrorMessage(error);
                }
            }
        };

        try {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Calling second waitForJob");
            }
            JobUtilities.waitForJob(jobId, requestCallback);
        }
        catch (RequestException e) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Second waitForJob failed");
            }
            addErrorMessage("CGI request failed: ", e);
        }
    }
}
