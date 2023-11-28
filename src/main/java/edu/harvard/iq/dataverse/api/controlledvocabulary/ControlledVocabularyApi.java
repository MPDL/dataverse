package edu.harvard.iq.dataverse.api.controlledvocabulary;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

@Singleton
@Path("controlledvocabulary")
public class ControlledVocabularyApi extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(ControlledVocabularyApi.class.getCanonicalName());


    private SolrClient solrClient;

    @PostConstruct
    public void init() {
        String urlString = "http://" + systemConfig.getSolrHostColonPort() + "/solr";
        solrClient = new HttpSolrClient.Builder(urlString).build();

    }

    @GET
    @Path("ror")
    public Response queryRorData(@QueryParam("q") String queryString) throws Exception
    {
        try {
            final StringBuilder sbb = new StringBuilder();
            sbb.append("autosuggest:(");
            sbb.append(queryString);
            sbb.append(")");

            final SolrQuery query = new SolrQuery(sbb.toString());
            query.setParam("defType", "edismax");
            query.addField("id");
            query.addField("name");
            query.setParam("q.op","AND");
            //Boost Max Planck related institutes
            query.setParam("bq", "relationships.label:(\"Max Planck\")^2");
            query.setRows(100);

            QueryRequest req = new QueryRequest(query);

            NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
            rawJsonResponseParser.setWriterType("json");
            req.setResponseParser(rawJsonResponseParser);

            NamedList<Object> resp = solrClient.request(req, "rordata");
            String jsonResponse = (String) resp.get("response");

            return Response.ok( jsonResponse )
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(e.getMessage()).build();
        }
    }
}
