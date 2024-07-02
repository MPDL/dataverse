package edu.harvard.iq.dataverse.pidproviders.doi;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataCiteMetadataUtilTest {

    @Test
     void testParseXml() throws ParserConfigurationException, IOException, SAXException {
        String testXML = this.createTestXML();

        org.w3c.dom.Document document = XmlMetadataTemplate.DataCiteMetadataUtil.parseXml(testXML);

        assertNotNull(document);
        assertEquals("UTF-8", document.getXmlEncoding());
        assertEquals("root", document.getDocumentElement().getNodeName());
        assertEquals("firstElement", document.getDocumentElement().getChildNodes().item(1).getNodeName());
    }

    @Test
    void testAppendElementToDocument() throws ParserConfigurationException{
        org.w3c.dom.Document document = this.createTestDocument();
        String parentTagName = "secondElement";
        String tageName = "firstChildOfSecondElement";
        String textContent = "text of firstChildOfSecondElement";

        XmlMetadataTemplate.DataCiteMetadataUtil.appendElementToDocument(document, parentTagName, tageName, textContent);

        assertEquals(1, document.getElementsByTagName(parentTagName).getLength());
        Node addedChildNode = document.getElementsByTagName(parentTagName).item(0).getFirstChild();
        assertEquals(tageName, addedChildNode.getNodeName());
        assertEquals(textContent, addedChildNode.getTextContent());
    }

    @Test
    void testPrettyPrintXML() throws Exception {
        org.w3c.dom.Document document = this.createTestDocument();

        String xmlMetadata = XmlMetadataTemplate.DataCiteMetadataUtil.prettyPrintXML(document, 4);

        String secondLine = xmlMetadata.split(System.lineSeparator())[1];
        assertEquals("<root>", secondLine);
        String thirdLine = xmlMetadata.split(System.lineSeparator())[2];
        assertEquals("    ", thirdLine.substring(0,4));
    }

    private String createTestXML(){
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <root>
                    <firstElement>Text of a TestNode.</firstElement>
                    <secondElement/>
                </root>
                """;
    }

    private org.w3c.dom.Document createTestDocument() throws ParserConfigurationException {
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = document.createElement("root");
        document.appendChild(rootElement);
        Element firstElement = document.createElement("firstElement");
        rootElement.appendChild(firstElement);
        firstElement.appendChild(document.createTextNode("Text of a TestNode."));
        Element secondElement = document.createElement("secondElement");
        rootElement.appendChild(secondElement);

        return document;
    }

}