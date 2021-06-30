package edu.harvard.iq.dataverse.api.batchjob;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.engine.command.impl.ImportFromFileSystemCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Component
@Path("batch/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class FileRecordJobResource extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(FileRecordJobResource.class.getName());

    @Autowired
    PermissionServiceBean permissionServiceBean;

    @Autowired
    DatasetServiceBean datasetService;

    @POST
    @Path("import/datasets/files/{identifier}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilesystemImport(@PathParam("identifier") String identifier,
            @QueryParam("mode") @DefaultValue("MERGE") String mode,
            /*@QueryParam("fileMode") @DefaultValue("package_file") String fileMode*/
            @QueryParam("uploadFolder") String uploadFolder,
            @QueryParam("totalSize") Long totalSize) {
        return response(req -> {
            ImportMode importMode = ImportMode.MERGE;
            // Switch to this if you ever need to use something other than MERGE.
            //ImportMode importMode = ImportMode.valueOf(mode);
            JsonObject jsonObject = execCommand(new ImportFromFileSystemCommand(req, findDatasetOrDie(identifier), uploadFolder, totalSize, importMode));
            String returnString = jsonObject.getString("message");
            if (!returnString.equals("FileSystemImportJob in progress")) {
                return error(Response.Status.INTERNAL_SERVER_ERROR, returnString);
            }
            return ok(Json.createObjectBuilder()
                    .add("message", returnString)
                    .add("executionId", jsonObject.getInt("executionId"))
            );
        });
    }

}
