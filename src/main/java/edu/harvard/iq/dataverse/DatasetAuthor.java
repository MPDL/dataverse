/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Comparator;

/**
 *
 * @author skraffmiller
 */
public class DatasetAuthor {
       
    public static Comparator<DatasetAuthor> DisplayOrder = new Comparator<DatasetAuthor>(){
        @Override
        public int compare(DatasetAuthor o1, DatasetAuthor o2) {
            return o1.getDisplayOrder()-o2.getDisplayOrder();
        }
    };
    
    private DatasetVersion datasetVersion;
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }
    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }

    //@NotBlank(message = "Please enter an Author Name for your dataset.")
    private DatasetField name;

    public DatasetField getName() {
        return this.name;
    }
    public void setName(DatasetField name) {
        this.name = name;
    }

    private int displayOrder;
    public int getDisplayOrder() {
        return this.displayOrder;
    }
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetField affiliation;
    public DatasetField getAffiliation() {
        return this.affiliation;
    }
    public void setAffiliation(DatasetField affiliation) {
        this.affiliation = affiliation;
    }

    private DatasetField affiliationId;
    public DatasetField getAffiliationId() {
        return this.affiliationId;
    }
    public void setAffiliationId(DatasetField affiliationId) {
        this.affiliationId = affiliationId;
    }

    private DatasetField affiliation2;
    public DatasetField getAffiliation2() {
        return this.affiliation2;
    }
    public void setAffiliation2(DatasetField affiliation2) {
        this.affiliation2 = affiliation2;
    }
    private DatasetField affiliation2Id;
    public DatasetField getAffiliation2Id() {
        return this.affiliation2Id;
    }
    public void setAffiliation2Id(DatasetField affiliation2Id) {
        this.affiliation2Id = affiliation2Id;
    }

    private DatasetField affiliation3;
    public DatasetField getAffiliation3() {
        return this.affiliation3;
    }
    public void setAffiliation3(DatasetField affiliation3) {
        this.affiliation3 = affiliation3;
    }
    private DatasetField affiliation3Id;
    public DatasetField getAffiliation3Id() {
        return this.affiliation3Id;
    }
    public void setAffiliation3Id(DatasetField affiliation3Id) {
        this.affiliation3Id = affiliation3Id;
    }

    private String idType;

    public String getIdType() {
        if ((this.idType == null || this.idType.isEmpty()) && (this.idValue != null && !this.idValue.isEmpty())){
            return ("ORCID");
        } else {
            return idType;
        }        
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
    
    private String idValue;
    
    
    public String getIdValue() {
        return idValue;
    }

    public void setIdValue(String idValue) {
        this.idValue = idValue;
    }

    public boolean isEmpty() {
        return ( (affiliation==null || affiliation.getValue().trim().equals(""))
            && (name==null || name.getValue().trim().equals(""))
           );
    }

    public String getIdentifierAsUrl() {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            return getIdentifierAsUrl(idType, idValue);
        }
        return null;
    }

    public static String getIdentifierAsUrl(String idType, String idValue) {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            try {
              ExternalIdentifier externalIdentifier = ExternalIdentifier.valueOf(idType);
              if (externalIdentifier.isValidIdentifier(idValue))
                return externalIdentifier.format(idValue);
            } catch (Exception e) {
                // non registered identifier
            }
        }
        return null;
    }
}
