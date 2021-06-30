package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MetadataBlock;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;

/**
 * Api bean for managing metadata blocks.
 * @author michael
 */
@Component
@Path("metadatablocks")
@Produces("application/json")
public class MetadataBlocks extends AbstractApiBean {
    
    @GET
    public Response list()  {
        return ok(metadataBlockSvc.listMetadataBlocks().stream().map(brief::json).collect(toJsonArray()));
    }
    
    @Path("{identifier}")
    @GET
    public Response getBlock( @PathParam("identifier") String idtf ) {
        MetadataBlock b = findMetadataBlock(idtf);
        
        return   (b != null ) ? ok(json(b)) : notFound("Can't find metadata block '" + idtf + "'");
    }
    
}
