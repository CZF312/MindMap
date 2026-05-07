package com.example.mindmap.service;

import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.scene.paint.Color;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MindMapFileService {
    public MindMap load(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(path.toFile());
        Element rootElement = document.getDocumentElement();
        if (!"mindmap".equals(rootElement.getTagName())) {
            throw new IOException("不是有效的 mindmap 文件");
        }

        MindMap map = new MindMap();
        map.setName(rootElement.getAttribute("name"));
        map.setLayoutType(parseLayout(rootElement.getAttribute("layout")));
        NodeList roots = rootElement.getElementsByTagName("node");
        if (roots.getLength() == 0) {
            throw new IOException("文件中没有中心节点");
        }
        map.setRoot(readNode((Element) roots.item(0)));
        map.setFilePath(path);
        map.setModified(false);
        map.bumpNextIdFromExistingNodes();
        return map;
    }

    public void save(MindMap map, Path path) throws Exception {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        map.setName(stripExtension(path.getFileName().toString()));
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("mindmap");
        root.setAttribute("version", "1");
        root.setAttribute("name", map.getName());
        root.setAttribute("layout", map.getLayoutType().name());
        document.appendChild(root);
        root.appendChild(writeNode(document, map.getRoot()));

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(document), new StreamResult(path.toFile()));
        map.setFilePath(path);
        map.setModified(false);
    }

    private Element writeNode(Document document, MindNode node) {
        Element element = document.createElement("node");
        element.setAttribute("id", node.getId());
        element.setAttribute("text", node.getText());
        element.setAttribute("x", String.valueOf(node.getX()));
        element.setAttribute("y", String.valueOf(node.getY()));
        element.setAttribute("offsetX", String.valueOf(node.getOffsetX()));
        element.setAttribute("offsetY", String.valueOf(node.getOffsetY()));
        element.setAttribute("collapsed", String.valueOf(node.isCollapsed()));
        element.setAttribute("fill", NodeStyle.toHex(node.getStyle().getFillColor()));
        element.setAttribute("border", NodeStyle.toHex(node.getStyle().getBorderColor()));
        element.setAttribute("textColor", NodeStyle.toHex(node.getStyle().getTextColor()));
        for (MindNode child : node.getChildren()) {
            element.appendChild(writeNode(document, child));
        }
        return element;
    }

    private MindNode readNode(Element element) {
        MindNode node = new MindNode(attribute(element, "id", "node-1"), attribute(element, "text", "新节点"));
        node.setX(parseDouble(element.getAttribute("x"), 0));
        node.setY(parseDouble(element.getAttribute("y"), 0));
        node.setOffsetX(parseDouble(element.getAttribute("offsetX"), 0));
        node.setOffsetY(parseDouble(element.getAttribute("offsetY"), 0));
        node.setCollapsed(Boolean.parseBoolean(element.getAttribute("collapsed")));
        node.setStyle(new NodeStyle(
                NodeStyle.fromHex(element.getAttribute("fill"), Color.WHITE),
                NodeStyle.fromHex(element.getAttribute("border"), Color.web("#CBD5E1")),
                NodeStyle.fromHex(element.getAttribute("textColor"), Color.web("#0F172A"))));
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && "node".equals(child.getTagName())) {
                node.addChild(readNode(child));
            }
        }
        return node;
    }

    private LayoutType parseLayout(String value) {
        try {
            return LayoutType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return LayoutType.AUTO;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String attribute(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
