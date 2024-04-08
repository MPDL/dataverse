package edu.harvard.iq.dataverse.pidproviders.doi;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlMetadataTemplate {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.DataCiteMetadataTemplate");
    private static String template;

    static {
        try (InputStream in = XmlMetadataTemplate.class.getResourceAsStream("datacite_metadata_template.xml")) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "datacite metadata template load error");
            logger.log(Level.SEVERE, "String " + e.toString());
            logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
            logger.log(Level.SEVERE, "cause " + e.getCause());
            logger.log(Level.SEVERE, "message " + e.getMessage());
        }
    }

    private String xmlMetadata;
    private String identifier;
    private List<String> datafileIdentifiers;
    private List<String> creators;
    private String title;
    private String publisher;
    private String publisherYear;
    private List<DatasetAuthor> authors;
    private String description;
    private List<String[]> contacts;
    private List<String[]> producers;

    public List<String[]> getProducers() {
        return producers;
    }

    public void setProducers(List<String[]> producers) {
        this.producers = producers;
    }

    public List<String[]> getContacts() {
        return contacts;
    }

    public void setContacts(List<String[]> contacts) {
        this.contacts = contacts;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DatasetAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<DatasetAuthor> authors) {
        this.authors = authors;
    }

    public XmlMetadataTemplate() {
    }

    public List<String> getDatafileIdentifiers() {
        return datafileIdentifiers;
    }

    public void setDatafileIdentifiers(List<String> datafileIdentifiers) {
        this.datafileIdentifiers = datafileIdentifiers;
    }

    public XmlMetadataTemplate(String xmlMetaData) {
        this.xmlMetadata = xmlMetaData;
        Document doc = Jsoup.parseBodyFragment(xmlMetaData);
        Elements identifierElements = doc.select("identifier");
        if (identifierElements.size() > 0) {
            identifier = identifierElements.get(0).html();
        }
        Elements creatorElements = doc.select("creatorName");
        creators = new ArrayList<>();
        for (Element creatorElement : creatorElements) {
            creators.add(creatorElement.html());
        }
        Elements titleElements = doc.select("title");
        if (titleElements.size() > 0) {
            title = titleElements.get(0).html();
        }
        Elements publisherElements = doc.select("publisher");
        if (publisherElements.size() > 0) {
            publisher = publisherElements.get(0).html();
        }
        Elements publisherYearElements = doc.select("publicationYear");
        if (publisherYearElements.size() > 0) {
            publisherYear = publisherYearElements.get(0).html();
        }
    }

    public String generateXML(DvObject dvObject) {
        // Can't use "UNKNOWN" here because DataCite will respond with "[facet
        // 'pattern'] the value 'unknown' is not accepted by the pattern '[\d]{4}'"
        String publisherYearFinal = "9999";
        // FIXME: Investigate why this.publisherYear is sometimes null now that pull
        // request #4606 has been merged.
        if (this.publisherYear != null) {
            // Added to prevent a NullPointerException when trying to destroy datasets when
            // using DataCite rather than EZID.
            publisherYearFinal = this.publisherYear;
        }
        xmlMetadata = template.replace("${identifier}", getIdentifier().trim()).replace("${title}", this.title)
                .replace("${publisher}", this.publisher).replace("${publisherYear}", publisherYearFinal)
                .replace("${description}", this.description);

        StringBuilder creatorsElement = new StringBuilder();
        if (authors != null && !authors.isEmpty()) {
            for (DatasetAuthor author : authors) {
                creatorsElement.append("<creator><creatorName>");
                creatorsElement.append(author.getName().getDisplayValue());
                creatorsElement.append("</creatorName>");

                if (author.getIdType() != null && author.getIdValue() != null && !author.getIdType().isEmpty()
                        && !author.getIdValue().isEmpty() && author.getAffiliation() != null
                        && !author.getAffiliation().getDisplayValue().isEmpty()) {

                    if (author.getIdType().equals("ORCID")) {
                        creatorsElement.append(
                                "<nameIdentifier schemeURI=\"https://orcid.org/\" nameIdentifierScheme=\"ORCID\">"
                                        + author.getIdValue() + "</nameIdentifier>");
                    }
                    if (author.getIdType().equals("ISNI")) {
                        creatorsElement.append(
                                "<nameIdentifier schemeURI=\"http://isni.org/isni/\" nameIdentifierScheme=\"ISNI\">"
                                        + author.getIdValue() + "</nameIdentifier>");
                    }
                    if (author.getIdType().equals("LCNA")) {
                        creatorsElement.append(
                                "<nameIdentifier schemeURI=\"http://id.loc.gov/authorities/names/\" nameIdentifierScheme=\"LCNA\">"
                                        + author.getIdValue() + "</nameIdentifier>");
                    }
                }
                if (author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
                    creatorsElement.append("<affiliation");
                    if(author.getAffiliationId() != null && !author.getAffiliationId().getDisplayValue().isEmpty()) {
                        creatorsElement.append(" schemeURI=\"https://ror.org/\" affiliationIdentifierScheme=\"ROR\" affiliationIdentifier=\""+ author.getAffiliationId().getValue() +"\"");
                    }
                    creatorsElement.append(">" + author.getAffiliation().getDisplayValue() + "</affiliation>");

                }
                if (author.getAffiliation2() != null && !author.getAffiliation2().getDisplayValue().isEmpty()) {
                    creatorsElement.append("<affiliation");
                    if(author.getAffiliation2Id() != null && !author.getAffiliation2Id().getDisplayValue().isEmpty()) {
                        creatorsElement.append(" schemeURI=\"https://ror.org/\" affiliationIdentifierScheme=\"ROR\" affiliationIdentifier=\""+ author.getAffiliation2Id().getValue() +"\"");
                    }
                    creatorsElement.append(">" + author.getAffiliation2().getDisplayValue() + "</affiliation>");

                }
                if (author.getAffiliation3() != null && !author.getAffiliation3().getDisplayValue().isEmpty()) {
                    creatorsElement.append("<affiliation");
                    if(author.getAffiliation3Id() != null && !author.getAffiliation3Id().getDisplayValue().isEmpty()) {
                        creatorsElement.append(" schemeURI=\"https://ror.org/\" affiliationIdentifierScheme=\"ROR\" affiliationIdentifier=\""+ author.getAffiliation3Id().getValue() +"\"");
                    }
                    creatorsElement.append(">" + author.getAffiliation3().getDisplayValue() + "</affiliation>");

                }
                creatorsElement.append("</creator>");
            }

        } else {
            creatorsElement.append("<creator><creatorName>").append(AbstractPidProvider.UNAVAILABLE)
                    .append("</creatorName></creator>");
        }

        xmlMetadata = xmlMetadata.replace("${creators}", creatorsElement.toString());

        StringBuilder contributorsElement = new StringBuilder();
        if (this.getContacts() != null) {
            for (String[] contact : this.getContacts()) {
                if (!contact[0].isEmpty()) {
                    contributorsElement.append("<contributor contributorType=\"ContactPerson\"><contributorName>"
                            + contact[0] + "</contributorName>");
                    if (!contact[1].isEmpty()) {
                        contributorsElement.append("<affiliation>" + contact[1] + "</affiliation>");
                    }
                    contributorsElement.append("</contributor>");
                }
            }
        }

        if (this.getProducers() != null) {
            for (String[] producer : this.getProducers()) {
                contributorsElement.append("<contributor contributorType=\"Producer\"><contributorName>" + producer[0]
                        + "</contributorName>");
                if (!producer[1].isEmpty()) {
                    contributorsElement.append("<affiliation>" + producer[1] + "</affiliation>");
                }
                contributorsElement.append("</contributor>");
            }
        }

        String relIdentifiers = generateRelatedIdentifiers(dvObject);

        xmlMetadata = xmlMetadata.replace("${relatedIdentifiers}", relIdentifiers);

        xmlMetadata = xmlMetadata.replace("{$contributors}", contributorsElement.toString());

        xmlMetadata = this.addFundingReferences(dvObject, xmlMetadata);

        return xmlMetadata;
    }

    private String addFundingReferences(DvObject dvObject, String xmlMetadata) {
        try {
            if (dvObject.isInstanceofDataset()) {
                Dataset dataset = (Dataset) dvObject;
                List<Map<String, String>> grantNumberChildValues = this.extractGrantNumberValues(dataset);
                if (!grantNumberChildValues.isEmpty()) {
                    org.w3c.dom.Document xmlDocument = DataCiteMetadataUtil.parseXml(xmlMetadata);
                    xmlDocument = this.appendFundingReferences(grantNumberChildValues, xmlDocument);
                    xmlMetadata = DataCiteMetadataUtil.prettyPrintXML(xmlDocument, 4);
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error adding fundingReferences to the DataCite Metadata: {0}", e.getMessage());
        }
        return xmlMetadata;
    }

    private List<Map<String, String>> extractGrantNumberValues(Dataset dataset) {
        List<Map<String, String>> grantNumberChildValues = new ArrayList<>();
        List<DatasetField> grantNumberDatasetFields = DataCiteMetadataUtil.searchForFirstLevelDatasetFields(dataset, DatasetFieldConstant.grantNumber);
        //There should only be one DatasetField with name 'grantNumber' (Premise: There are values for grantNumber)
        if(!grantNumberDatasetFields.isEmpty()){
            DatasetField grantNumber = grantNumberDatasetFields.get(0);
            grantNumberChildValues = DataCiteMetadataUtil.extractCompoundValueChildDatasetFieldValues(grantNumber);
        }
        return grantNumberChildValues;
    }

    /**
     * <pre>
     * Appends fundingReferences to the DataCite xml.
     * Mappings:
     * - grantNumberAgency -> funderName
     * - grantNumberValue -> awardNumber
     * </pre>
     *
     * @param grantNumberChildValues
     * @param xmlDocument
     * @return The xmlDocument with fundingReferences
     */
    private org.w3c.dom.Document appendFundingReferences(List<Map<String, String>> grantNumberChildValues, org.w3c.dom.Document xmlDocument) {
        for (Map<String, String> childValue : grantNumberChildValues) {
            // funderName (=grantNumberAgency) is a required subfield of fundingReference
            if (childValue.containsKey(DatasetFieldConstant.grantNumberAgency)) {
                if(xmlDocument.getElementsByTagName("fundingReferences").getLength() == 0){
                    DataCiteMetadataUtil.appendElementToDocument(xmlDocument, "resource", "fundingReferences", null);
                }
                DataCiteMetadataUtil.appendElementToDocument(xmlDocument, "fundingReferences", "fundingReference", null);
                DataCiteMetadataUtil.appendElementToDocument(xmlDocument, "fundingReference", "funderName", childValue.get(DatasetFieldConstant.grantNumberAgency));
                if (childValue.containsKey(DatasetFieldConstant.grantNumberValue)) {
                    DataCiteMetadataUtil.appendElementToDocument(xmlDocument, "fundingReference", "awardNumber", childValue.get(DatasetFieldConstant.grantNumberValue));
                }
            }
        }
        return xmlDocument;
    }

    private String generateRelatedIdentifiers(DvObject dvObject) {

        StringBuilder sb = new StringBuilder();
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;
            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    if (dataFile.getGlobalId() != null) {
                        if (sb.toString().isEmpty()) {
                            sb.append("<relatedIdentifiers>");
                        }
                        sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"HasPart\">"
                                + dataFile.getGlobalId() + "</relatedIdentifier>");
                    }
                }

                if (!sb.toString().isEmpty()) {
                    sb.append("</relatedIdentifiers>");
                }
            }
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            sb.append("<relatedIdentifiers>");
            sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"IsPartOf\"" + ">"
                    + df.getOwner().getGlobalId() + "</relatedIdentifier>");
            sb.append("</relatedIdentifiers>");
        }
        return sb.toString();
    }

    public void generateFileIdentifiers(DvObject dvObject) {

        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;

            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    datafileIdentifiers.add(dataFile.getIdentifier());
                    int x = xmlMetadata.indexOf("</relatedIdentifiers>") - 1;
                    xmlMetadata = xmlMetadata.replace("{relatedIdentifier}", dataFile.getIdentifier());
                    xmlMetadata = xmlMetadata.substring(0, x) + "<relatedIdentifier relatedIdentifierType=\"hasPart\" "
                            + "relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>"
                            + template.substring(x, template.length() - 1);

                }

            } else {
                xmlMetadata = xmlMetadata.replace(
                        "<relatedIdentifier relatedIdentifierType=\"hasPart\" relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>",
                        "");
            }
        }
    }

    public static String getTemplate() {
        return template;
    }

    public static void setTemplate(String template) {
        XmlMetadataTemplate.template = template;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getCreators() {
        return creators;
    }

    public void setCreators(List<String> creators) {
        this.creators = creators;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisherYear() {
        return publisherYear;
    }

    public void setPublisherYear(String publisherYear) {
        this.publisherYear = publisherYear;
    }

    class DataCiteMetadataUtil {

        public static org.w3c.dom.Document parseXml(String xml) throws ParserConfigurationException, IOException, SAXException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(new InputSource(new StringReader(xml)));

            return document;
        }

        /**
         * Append Element to the last parent element in order.
         *
         * @param document
         * @param parentTagName
         * @param tagName
         * @param textContent
         */
        public static void appendElementToDocument(org.w3c.dom.Document document, String parentTagName, String tagName, String textContent) {
            org.w3c.dom.Element element = document.createElement(tagName);
            if(textContent != null && !textContent.isEmpty()) {
                element.setTextContent(textContent);
            }
            org.w3c.dom.NodeList parentElements = document.getElementsByTagName(parentTagName);
            if(parentElements.getLength() > 0){
                org.w3c.dom.Element lastParentElement = (org.w3c.dom.Element) parentElements.item(parentElements.getLength() - 1);
                lastParentElement.appendChild(element);
            }
        }

        public static String prettyPrintXML(org.w3c.dom.Document document, int indent) throws Exception {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            InputStream inputStream = XmlMetadataTemplate.class.getResourceAsStream("prettyprint.xsl");
            String prettyPrintXsl = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(prettyPrintXsl)));
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        }

        /**
         * Search for a fist-level DatasetFields by name.
         *
         * @param dataset
         * @param datasetFieldName
         * @return List of DatasetFields with the given name.
         */
        public static List<DatasetField> searchForFirstLevelDatasetFields(Dataset dataset, String datasetFieldName) {
            List<DatasetField> datasetFields = new ArrayList<>();
            for (DatasetField datasetField : dataset.getLatestVersion().getDatasetFields()) {
                if (datasetField.getDatasetFieldType().getName().equals(datasetFieldName)) {
                    datasetFields.add(datasetField);
                }
            }
            return datasetFields;
        }

        public static List<Map<String, String>> extractCompoundValueChildDatasetFieldValues(DatasetField datasetField){
            List<Map<String, String>> fieldValues = new ArrayList<>();
            for (DatasetFieldCompoundValue compoundValue : datasetField.getDatasetFieldCompoundValues()) {
                fieldValues.add(DataCiteMetadataUtil.extractChildDatasetFieldValues(compoundValue));
            }
            return fieldValues;
        }

        public static Map<String, String> extractChildDatasetFieldValues(DatasetFieldCompoundValue datasetFieldCompoundValue) {
            Map<String, String> datasetFieldValues = new HashMap<>();
            for (DatasetField childDatasetField : datasetFieldCompoundValue.getChildDatasetFields()) {
                datasetFieldValues.put(childDatasetField.getDatasetFieldType().getName(), childDatasetField.getValue());
            }
            return datasetFieldValues;
        }

    }

}