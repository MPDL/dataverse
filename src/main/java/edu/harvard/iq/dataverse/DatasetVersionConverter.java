/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.faces.annotation.ManagedProperty;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author skraffmi
 */
@FacesConverter("datasetVersionConverter")
public class DatasetVersionConverter implements Converter {
    
    //@Autowired
	@Inject
    DatasetVersionServiceBean datasetVersionService;
    
    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
                if (value == null || value.equals("")) {
            return "";
        } else {                  
            return datasetVersionService.find(new Long(value));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
                if (value == null || value.equals("")) {
            return "";
        } else {
            String stringToReturn = ((DatasetVersion) value).getId().toString();
            return stringToReturn;
        }
    }
    
}
