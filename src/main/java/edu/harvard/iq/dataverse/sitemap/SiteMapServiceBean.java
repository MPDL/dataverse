package edu.harvard.iq.dataverse.sitemap;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;

@Service
public class SiteMapServiceBean {

    @Async
    public void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {
        SiteMapUtil.updateSiteMap(dataverses, datasets);
    }

}
