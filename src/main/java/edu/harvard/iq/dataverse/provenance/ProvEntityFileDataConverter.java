package edu.harvard.iq.dataverse.provenance;

import org.springframework.stereotype.Component;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 *
 * @author madunlap
 * To sort our entity objects in the provenance bundle dropdown
 */


@FacesConverter(value = "provEntityFileDataConverter", managed = true)
public class ProvEntityFileDataConverter implements Converter{

    
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        ProvPopupFragmentBean provBean = context.getApplication().evaluateExpressionGet(context, "#{provPopupFragmentBean}", ProvPopupFragmentBean.class);
        return provBean.getEntityByEntityName(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((ProvEntityFileData) value).getEntityName();
        }
        
    }
}
