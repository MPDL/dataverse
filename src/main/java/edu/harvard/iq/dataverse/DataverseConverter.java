/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import org.springframework.stereotype.Component;

import javax.faces.annotation.ManagedProperty;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author skraffmiller
 */

@Component
public class DataverseConverter implements Converter {

    
    //@Autowired
	@Inject
    DataverseServiceBean dataverseService;

    

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return dataverseService.find(new Long(submittedValue));
        //return dataverseService.findByAlias(submittedValue);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((Dataverse) value).getId().toString();
            //return ((Dataverse) value).getAlias();
        }
    }
}
