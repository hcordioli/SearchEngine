package searchEngine;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.text.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ConvertStrToXML {
	public ConvertStrToXML(StringBuffer sb) throws SAXException, ParserConfigurationException, IOException  {
		// TODO Auto-generated constructor stub
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return;
	}
}
