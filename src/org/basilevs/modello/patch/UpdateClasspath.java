package org.basilevs.modello.patch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Scan current directory and adds target/generated-sources/* to all found .classpath files
 */
class UpdateClasspath {
	public static void main(String [] args) throws IOException {
		UpdateClasspath instance = new UpdateClasspath();
		instance.scanDirectory(Path.of(""));
		
	}

	private void scanDirectory(Path root) throws IOException {
		try (Stream<Path> directroryStream = Files.walk(root)) {
			for (Path entry: (Iterable<Path>)directroryStream::iterator) {
				if (entry.getFileName().toString().equals(".classpath")) {
					updateFile(entry);
				}
			}
		}
	}

	private void updateFile(Path classPathFile) throws IOException {
		Path root = classPathFile.getParent();
		Path generatedRoot = root.resolve("target").resolve("generated-sources");
		if (!Files.exists(generatedRoot)) {
			return;
		}
		List<Path> children;
		try (Stream<Path> list = Files.list(generatedRoot)) {
			children= list.toList();
		}
		List<String> relativePath = children.stream().map(root::relativize).map(Path::toString).toList();
		updateXml(classPathFile, xmlElement -> {
			List<String> existing = ClasspathParser.extractAllClassPathes(xmlElement);
			for (String child: relativePath) {
				if (!existing.contains(child)) {
					ClasspathParser.appendClasspath(xmlElement, "src", child);
				}
			}
		});
		
	}

	private void updateXml(Path classPathFile, Consumer<Element> mutate) throws IOException {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Document document = builder.parse(classPathFile.toFile());
			mutate.accept(document.getDocumentElement());
		    DOMSource dom = new DOMSource(document);
		    StreamResult result = new StreamResult(classPathFile.toFile());
		    transformer.transform(dom, result);
		} catch(ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError | SAXException e) {
			throw new IllegalStateException(e);
		}
	}
	
}