/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Service;

/**
 *
 * @author skraffmiller
 */
@Service
public class ControlledVocabularyValueServiceBean implements java.io.Serializable {

    @PersistenceContext
    private EntityManager em;
    
    public List<ControlledVocabularyValue> findByDatasetFieldTypeId(Long dsftId) {

        String queryString = "select o from ControlledVocabularyValue as o where o.datasetFieldType.id = " + dsftId + " ";
        TypedQuery<ControlledVocabularyValue> query = em.createQuery(queryString, ControlledVocabularyValue.class);
        return query.getResultList();
        
    }
    
}
