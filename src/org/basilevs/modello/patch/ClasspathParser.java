package org.basilevs.modello.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class ClasspathParser {
	private ClasspathParser() {
	}
	public static List<String> extractAllClassPathes(Element xmlElement) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "classpathentry/@path";
		NodeList result;
		try {
			result = (NodeList) xPath.compile(expression).evaluate(xmlElement, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new AssertionError(e);
		}
		return stream(result).map(Node::getTextContent).toList();
	}
	
	public static void appendClasspath(Element xmlElement, String kind, String path) {
		Document document = xmlElement.getOwnerDocument();
		Node node = document.createElement("classpathentry");
		Node attribute = document.createAttribute("kind");
		attribute.setNodeValue(kind);
		node.getAttributes().setNamedItem(attribute);
		attribute = document.createAttribute("path");
		attribute.setNodeValue(path);
		node.getAttributes().setNamedItem(attribute);
		xmlElement.appendChild(node);
	}
	
	@Test
	public void testParse() throws ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError, SAXException, IOException {
		Element element = parseXmlResource();
		List<String> list = extractAllClassPathes(element);
		Assertions.assertEquals(9, list.size());
	}
	
	@Test
	public void testAppend() throws ParserConfigurationException {
		Element element = parseXmlResource();
		appendClasspath(element, "dumy", "test_path");
		Assertions.assertTrue(extractAllClassPathes(element).contains("test_path"));
	}
	
	private Element parseXmlResource() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			try (InputStream input = getClass().getResourceAsStream(".classpath")) {
				document = builder.parse(input);
			}
			Element element = document.getDocumentElement();
			return element;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static Stream<Node> stream(NodeList list) {
		return IntStream.range(0, list.getLength()).mapToObj(list::item);
	}

}
