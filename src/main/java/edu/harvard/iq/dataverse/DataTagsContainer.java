package edu.harvard.iq.dataverse;

import javax.json.JsonObject;

import org.springframework.stereotype.Component;

/**
 *
 * @author Naomi
 */
@Component
public class DataTagsContainer {
    
    private String datasetName;
    private JsonObject tag;
    
    
    /**
     * Creates a new instance of DataTagsContainer
     */
    public DataTagsContainer() {
        datasetName = "";
        tag = null;
    }
    
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    
    public String getDatasetName() {
        return datasetName;
    }
    
    public void setTag(JsonObject tag) {
        this.tag = tag;
    }
    
    public JsonObject getTag() {
        return tag;
    }
    
}
