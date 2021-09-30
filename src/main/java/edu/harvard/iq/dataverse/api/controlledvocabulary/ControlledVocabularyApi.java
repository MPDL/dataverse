package edu.harvard.iq.dataverse.api.controlledvocabulary;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
            query.addField("id");
            query.addField("name");
            query.setParam("q.op","AND");
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
