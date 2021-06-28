/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.json.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

/**
 *
 * @author Leonid Andreev
 */

@Path("harvest/server/oaisets")
public class HarvestingServer extends AbstractApiBean {
    @Autowired
    OAISetServiceBean oaiSetService;

    private static final Logger logger = Logger.getLogger(HarvestingServer.class.getName()); 
    
    // TODO: this should be available to admin only.

    @GET
    @Path("/")
    public Response oaiSets(@QueryParam("key") String apiKey) throws IOException {
        

        List<OAISet> oaiSets = null;
        try {
            oaiSets = oaiSetService.findAll();
        } catch (Exception ex) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Caught an exception looking up available OAI sets; " + ex.getMessage());
        }

        if (oaiSets == null) {
            // returning an empty list:
            return ok(jsonObjectBuilder().add("oaisets", ""));
        }

        JsonArrayBuilder hcArr = Json.createArrayBuilder();

        for (OAISet set : oaiSets) {
            hcArr.add(oaiSetAsJson(set));
        }

        return ok(jsonObjectBuilder().add("oaisets", hcArr));
    }
    
    @GET
    @Path("{specname}")
    public Response oaiSet(@PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException {
        
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        if (set == null) {
            return error(Response.Status.NOT_FOUND, "OAI set " + spec + " not found.");
        }
                
        try {
            return ok(oaiSetAsJson(set));  
        } catch (Exception ex) {
            logger.warning("Unknown exception caught while trying to format OAI set " + spec + " as json: "+ex.getMessage());
            return error( Response.Status.BAD_REQUEST, 
                    "Internal error: failed to produce output for OAI set " + spec + ".");
        }
    }
   
    /**
     * create an OAI set from spec in path and other parameters from POST body
     * (as JSON). {"name":$set_name,
     * "description":$optional_set_description,"definition":$set_search_query_string}.
     */
    @POST
    @Path("{specname}")
    public Response createOaiSet(String jsonBody, @PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        /*
	     * authorization modeled after the UI (aka HarvestingSetsPage)
         */
        AuthenticatedUser dvUser;
        try {
            dvUser = findAuthenticatedUserOrDie();
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        if (!dvUser.isSuperuser()) {            
            return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.superUser.required"));
        }

        StringReader rdr = new StringReader(jsonBody);
        
	try( JsonReader jrdr = Json.createReader(rdr) )
	{
		JsonObject json = jrdr.readObject();

		OAISet set = new OAISet();
		//Validating spec 
		if (!StringUtils.isEmpty(spec)) {
			if (spec.length() > 30) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.sizelimit"));
			}
			if (!Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", spec)) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.invalid"));
				// If it passes the regex test, check 
			}
			if (oaiSetService.findBySpec(spec) != null) {
				return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.alreadyused"));
			}

		} else {
			return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
		}
		set.setSpec(spec);
		String name, desc, defn;

		try {
			name = json.getString("name");
		} catch (NullPointerException npe_name) {
			return badRequest(BundleUtil.getStringFromBundle("harvestserver.newSetDialog.setspec.required"));
		}
		try {
			defn = json.getString("definition");
		} catch (NullPointerException npe_defn) {
			throw new JsonParseException("definition unspecified");
		}
		try {
			desc = json.getString("description");
		} catch (NullPointerException npe_desc) {
			desc = ""; //treating description as optional
		}
		set.setName(name);
		set.setDescription(desc);
		set.setDefinition(defn);
		oaiSetService.save(set);
		return created("/harvest/server/oaisets" + spec, oaiSetAsJson(set));
	}
	
    }

    @PUT
    @Path("{nickName}")
    public Response modifyOaiSet(String jsonBody, @PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException, JsonParseException {
        // TODO:
        // ...
        return created("/harvest/server/oaisets" + spec, null);
    }
    
    @DELETE
    @Path("{specname}")
    public Response deleteOaiSet(@PathParam("specname") String spec, @QueryParam("key") String apiKey) {
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        if (set == null) {
            return error(Response.Status.NOT_FOUND, "OAI set " + spec + " not found.");
        }
        
        try {
            oaiSetService.setDeleteInProgress(set.getId());
            oaiSetService.remove(set.getId());
        } catch (Exception ex) {
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to delete OAI set " + spec + "; " + ex.getMessage());
        }
        
        return ok("OAI Set " + spec + " deleted");
    
    }
    
    @GET
    @Path("{specname}/datasets")
    public Response oaiSetListDatasets(@PathParam("specname") String spec, @QueryParam("key") String apiKey) throws IOException {
        OAISet set = null;  
        try {
            set = oaiSetService.findBySpec(spec);
        } catch (Exception ex) {
            logger.warning("Exception caught looking up OAI set " + spec + ": " + ex.getMessage());
            return error( Response.Status.BAD_REQUEST, "Internal error: failed to look up OAI set " + spec + ".");
        }
        
        return ok("");
        
    }
    
    /* Auxiliary, helper methods: */
    public static JsonArrayBuilder oaiSetsAsJsonArray(List<OAISet> oaiSets) {
        JsonArrayBuilder hdArr = Json.createArrayBuilder();

        for (OAISet set : oaiSets) {
            hdArr.add(oaiSetAsJson(set));
        }
        return hdArr;
    }

    public static JsonObjectBuilder oaiSetAsJson(OAISet set) {
        if (set == null) {
            return null;
        }

        return jsonObjectBuilder().add("name", set.getName()).
                add("spec", set.getSpec()).
                add("description", set.getDescription()).
                add("definition", set.getDefinition()).
                add("version", set.getVersion());
    }

}
