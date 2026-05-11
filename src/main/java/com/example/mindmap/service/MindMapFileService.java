package com.example.mindmap.service;

import com.example.mindmap.model.LayoutType;
import com.example.mindmap.model.ConnectionShape;
import com.example.mindmap.model.ConnectionStyle;
import com.example.mindmap.model.MindMap;
import com.example.mindmap.model.MindNode;
import com.example.mindmap.model.NodeStyle;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
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
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(path.toFile());
        Element rootElement = document.getDocumentElement();
        if (!"mindmap".equals(rootElement.getTagName())) {
            throw new IOException("不是有效的 mindmap 文件");
        }

        MindMap map = new MindMap();
        map.setName(rootElement.getAttribute("name"));
        map.setLayoutType(parseLayout(rootElement.getAttribute("layout")));
        map.setCanvasColor(NodeStyle.fromHex(rootElement.getAttribute("background"), Color.WHITE));
        map.setCanvasImageUri(attribute(rootElement, "canvasImage", null));
        map.setConnectionColor(NodeStyle.fromHex(rootElement.getAttribute("connectionColor"), Color.web("#94A3B8")));
        map.setConnectionWidth(parseDouble(rootElement.getAttribute("connectionWidth"), 2.2));
        map.setConnectionDashed(Boolean.parseBoolean(rootElement.getAttribute("connectionDashed")));
        map.setConnectionShape(parseConnectionShape(rootElement.getAttribute("connectionShape")));
        Element rootNode = firstDirectNode(rootElement);
        if (rootNode == null) {
            throw new IOException("文件中没有中心节点");
        }
        map.setRoot(readNode(rootNode, map.defaultConnectionStyle()));
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
        root.setAttribute("background", NodeStyle.toHex(map.getCanvasColor()));
        if (map.getCanvasImageUri() != null) {
            root.setAttribute("canvasImage", map.getCanvasImageUri());
        }
        root.setAttribute("connectionColor", NodeStyle.toHex(map.getConnectionColor()));
        root.setAttribute("connectionWidth", String.valueOf(map.getConnectionWidth()));
        root.setAttribute("connectionDashed", String.valueOf(map.isConnectionDashed()));
        root.setAttribute("connectionShape", map.getConnectionShape().name());
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
        element.setAttribute("width", String.valueOf(node.getWidth()));
        element.setAttribute("height", String.valueOf(node.getHeight()));
        element.setAttribute("customSize", String.valueOf(node.hasCustomSize()));
        element.setAttribute("collapsed", String.valueOf(node.isCollapsed()));
        element.setAttribute("fill", NodeStyle.toHex(node.getStyle().getFillColor()));
        element.setAttribute("border", NodeStyle.toHex(node.getStyle().getBorderColor()));
        element.setAttribute("textColor", NodeStyle.toHex(node.getStyle().getTextColor()));
        element.setAttribute("fontFamily", node.getStyle().getFontFamily());
        element.setAttribute("fontSize", String.valueOf(node.getStyle().getFontSize()));
        element.setAttribute("bold", String.valueOf(node.getStyle().isBold()));
        element.setAttribute("italic", String.valueOf(node.getStyle().isItalic()));
        element.setAttribute("underline", String.valueOf(node.getStyle().isUnderline()));
        element.setAttribute("strikethrough", String.valueOf(node.getStyle().isStrikethrough()));
        element.setAttribute("alignment", node.getStyle().getAlignment().name());
        element.setAttribute("connectionColor", NodeStyle.toHex(node.getConnectionStyle().getColor()));
        element.setAttribute("connectionWidth", String.valueOf(node.getConnectionStyle().getWidth()));
        element.setAttribute("connectionDashed", String.valueOf(node.getConnectionStyle().isDashed()));
        element.setAttribute("connectionShape", node.getConnectionStyle().getShape().name());
        for (MindNode child : node.getChildren()) {
            element.appendChild(writeNode(document, child));
        }
        return element;
    }

    private MindNode readNode(Element element, ConnectionStyle defaultConnectionStyle) {
        MindNode node = new MindNode(attribute(element, "id", "node-1"), attribute(element, "text", "新节点"));
        node.setX(parseDouble(element.getAttribute("x"), 0));
        node.setY(parseDouble(element.getAttribute("y"), 0));
        node.setWidth(parseDouble(element.getAttribute("width"), 150));
        node.setHeight(parseDouble(element.getAttribute("height"), 54));
        node.setCustomSize(Boolean.parseBoolean(element.getAttribute("customSize")));
        node.setOffsetX(parseDouble(element.getAttribute("offsetX"), 0));
        node.setOffsetY(parseDouble(element.getAttribute("offsetY"), 0));
        node.setCollapsed(Boolean.parseBoolean(element.getAttribute("collapsed")));
        NodeStyle style = new NodeStyle(
                NodeStyle.fromHex(element.getAttribute("fill"), Color.WHITE),
                NodeStyle.fromHex(element.getAttribute("border"), Color.web("#CBD5E1")),
                NodeStyle.fromHex(element.getAttribute("textColor"), Color.web("#0F172A")));
        style.setFontFamily(attribute(element, "fontFamily", "Microsoft YaHei UI"));
        style.setFontSize(parseDouble(element.getAttribute("fontSize"), 13));
        style.setBold(Boolean.parseBoolean(element.getAttribute("bold")));
        style.setItalic(Boolean.parseBoolean(element.getAttribute("italic")));
        style.setUnderline(Boolean.parseBoolean(element.getAttribute("underline")));
        style.setStrikethrough(Boolean.parseBoolean(element.getAttribute("strikethrough")));
        style.setAlignment(parseAlignment(element.getAttribute("alignment")));
        node.setStyle(style);
        ConnectionStyle connectionStyle = new ConnectionStyle();
        connectionStyle.setColor(NodeStyle.fromHex(element.getAttribute("connectionColor"), defaultConnectionStyle.getColor()));
        connectionStyle.setWidth(parseDouble(element.getAttribute("connectionWidth"), defaultConnectionStyle.getWidth()));
        connectionStyle.setDashed(element.hasAttribute("connectionDashed")
                ? Boolean.parseBoolean(element.getAttribute("connectionDashed"))
                : defaultConnectionStyle.isDashed());
        connectionStyle.setShape(element.hasAttribute("connectionShape")
                ? parseConnectionShape(element.getAttribute("connectionShape"))
                : defaultConnectionStyle.getShape());
        node.setConnectionStyle(connectionStyle);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && "node".equals(child.getTagName())) {
                node.addChild(readNode(child, defaultConnectionStyle));
            }
        }
        return node;
    }

    private Element firstDirectNode(Element rootElement) {
        NodeList children = rootElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && "node".equals(child.getTagName())) {
                return child;
            }
        }
        return null;
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

    private TextAlignment parseAlignment(String value) {
        try {
            return TextAlignment.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return TextAlignment.CENTER;
        }
    }

    private ConnectionShape parseConnectionShape(String value) {
        try {
            return ConnectionShape.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return ConnectionShape.CURVE;
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
