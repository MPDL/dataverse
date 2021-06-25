package edu.harvard.iq.dataverse.export.ddi;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the DdiExportUtil once it's ready.
     */
    @Component
    public class DdiExportUtilHelper {

        @Autowired SettingsServiceBean settingsSvc;
        
        @PostConstruct
        public void injectService() {
            DdiExportUtil.injectSettingsService(settingsSvc);
        }
    }