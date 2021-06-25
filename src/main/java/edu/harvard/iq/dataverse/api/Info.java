package edu.harvard.iq.dataverse.api;

import javax.json.Json;
import javax.json.JsonValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

@Path("info")
public class Info extends AbstractApiBean {

    @Autowired
    SettingsServiceBean settingsService;
    
    @Autowired
    SystemConfig systemConfig;

    @GET
    @Path("settings/:DatasetPublishPopupCustomText")
    public Response getDatasetPublishPopupCustomText() {
        String setting = settingsService.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        if (setting != null) {
            return ok(Json.createObjectBuilder().add("message", setting));
        } else {
            return notFound("Setting " + SettingsServiceBean.Key.DatasetPublishPopupCustomText + " not found");
        }
    }
    
    @GET
    @Path("version")
    public Response getInfo() {
        String versionStr = systemConfig.getVersion(true);
        String[] comps = versionStr.split("build",2);
        String version = comps[0].trim();
        JsonValue build = comps.length > 1 ? Json.createArrayBuilder().add(comps[1].trim()).build().get(0) : JsonValue.NULL;
        
        return response( req -> ok( Json.createObjectBuilder().add("version", version)
                                                              .add("build", build)));
    }
    
    @GET
    @Path("server")
    public Response getServer() {
        return response( req -> ok(systemConfig.getDataverseServer()));
    }
    
    @GET
    @Path("apiTermsOfUse")
    public Response getTermsOfUse() {
        return response( req -> ok(systemConfig.getApiTermsOfUse()));
    }
}
