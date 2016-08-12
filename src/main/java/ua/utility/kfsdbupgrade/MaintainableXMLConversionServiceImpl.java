/*
 * Copyright 2014 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.utility.kfsdbupgrade;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
    
public class MaintainableXMLConversionServiceImpl {
	private static final Logger LOGGER = Logger.getLogger(MaintainableXMLConversionServiceImpl.class);
	private static final String SERIALIZATION_ATTRIBUTE = "serialization";
	private static final String CLASS_ATTRIBUTE = "class";
	private static final String MAINTENANCE_ACTION_ELEMENT_NAME = "maintenanceAction";
    private static final String OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME = "oldMaintainableObject";
    private static final String NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME = "newMaintainableObject";

	/**
	 * Populated by the <code>pattern</code> elements in the <code>rule</code>
	 * named <code>maint_doc_classname_changes</code> in {@link #rulesXmlFile}.
	 * See the {@link #rulesXmlFile} for more detail.
	 */
    private Map<String, String> classNameRuleMap;
	/**
	 * Populated by the <code>pattern</code> elements in the <code>rule</code>
	 * named <code>maint_doc_changed_class_properties</code> in
	 * {@link #rulesXmlFile}. {@link #setupConfigurationMaps()} also
	 * pre-populates with some values that are applicable to all BOs. See the
	 * {@link #rulesXmlFile} for more detail.
	 */
	private Map<String, Map<String, String>> classPropertyRuleMap;
	/**
	 * Populated by the <code>pattern</code> elements in the <code>rule</code>
	 * named <code>maint_doc_date_changes</code> in {@link #rulesXmlFile}. See
	 * the {@link #rulesXmlFile} for more detail.
	 */
    private Map<String, String> dateRuleMap;
	/**
	 * {@link File} containing the rule maps that will be used to transform the
	 * maintainable document XML.
	 */
	private final File rulesXmlFile;
	/**
	 * {@link Set} of {@link String}s representing classnames to ignore during
	 * transformation. Values are hardcoded and passed in during construction.
	 */
    private Set<String> ignoreClassSet = new HashSet<String>();
	/**
	 * If a {@link String} classname that begins with <code>edu.arizona</code>
	 * or <code>com.rsmart</code> is encountered in {@link #isValidClass(Class)}
	 * , that classname will be added to this {@link Set}. If it's the first
	 * time such a class has been encountered, it will be logged that that class
	 * is being skipped by processing.
	 */
    private Set <String> uaMaintenanceDocClasses = new HashSet <String>();
    
	/**
	 * Constructor
	 * 
	 * @param rulesXmlFile
	 *            Value for {@link #rulesXmlFile}
	 */
	public MaintainableXMLConversionServiceImpl(File rulesXmlFile) {
        this.rulesXmlFile = rulesXmlFile;
        
        ignoreClassSet.add("org.kuali.rice.kim.api.identity.Person");
        ignoreClassSet.add("org.kuali.rice.krad.bo.PersistableBusinessObjectExtension");
        ignoreClassSet.add("org.kuali.rice.core.api.util.type.KualiInteger");
        ignoreClassSet.add("org.kuali.rice.core.api.util.type.KualiPercent");
     
        // these 2 classes try to call Spring services in the default constructor which causes problems. 
        // in this context (no spring). The classes are relatively simple so bypassing them should not be an issue
        ignoreClassSet.add("org.kuali.kfs.coa.businessobject.IndirectCostRecoveryRate");
        ignoreClassSet.add("org.kuali.kfs.module.purap.businessobject.PurchaseOrderContractLanguage");

		// ignore the builtin xml #text
		ignoreClassSet.add("#text");
     }

	/**
	 * Transforms the given <code>xml</code> that is in KFS3 format to KFS6
	 * format.
	 * 
	 * @param xml
	 *            {@link String} of the XML to transform
	 * @return {@link String} of the transformed XML
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown
	 */
	public String transformMaintainableXML(String xml) throws Exception {
	    String beginning = StringUtils.substringBefore(xml, "<" + OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">");
        String oldMaintainableObjectXML = StringUtils.substringBetween(xml, "<" + OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">", "</" + OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">");
        String newMaintainableObjectXML = StringUtils.substringBetween(xml, "<" + NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">", "</" + NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">");
        String ending = StringUtils.substringAfter(xml, "</" + NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">");

        String convertedOldMaintainableObjectXML = transformSection(oldMaintainableObjectXML);
        String convertedNewMaintainableObjectXML = transformSection(newMaintainableObjectXML);

        String convertedXML =  beginning +
            "<" + OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">" + convertedOldMaintainableObjectXML +  "</" + OLD_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">" +
            "<" + NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">" + convertedNewMaintainableObjectXML +  "</" + NEW_MAINTAINABLE_OBJECT_ELEMENT_NAME + ">" +
            ending;
        return convertedXML;
	}

	/**
	 * Transforms the given <code>xml</code> section from KFS3 format to KFS6
	 * format.
	 * 
	 * @param xml
	 *            {@link String} of the XML to transform
	 * @return {@link String} of the transformed XML
	 * @throws Exception
	 *             Any {@link Exception}s encountered will be rethrown.
	 */
    private String transformSection(String xml) throws Exception {
    	String rawXml = xml;
        String maintenanceAction = StringUtils.substringBetween(xml, "<" + MAINTENANCE_ACTION_ELEMENT_NAME + ">", "</" + MAINTENANCE_ACTION_ELEMENT_NAME + ">");
        xml = StringUtils.substringBefore(xml, "<" + MAINTENANCE_ACTION_ELEMENT_NAME + ">");

        xml = upgradeBONotes(xml);
        
        if (classNameRuleMap == null) {
            setRuleMaps();
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document;
		try {
			document = db.parse(new InputSource(new StringReader(xml)));
		} catch (SAXParseException ex) {
			String eol = System.getProperty("line.separator");
			String exMsg = "Failed in db.parse(new InputSource(new StringReader(xml))) where xml=" + xml + eol + 
					       "of maintenanceAction = " +  maintenanceAction + eol +
					       "contained in rawXml = " +  rawXml;									
			throw new SAXParseException(exMsg, null, ex);
		}

        for(Node childNode = document.getFirstChild(); childNode != null;) {
			/*
			 * TODO investigate; this doesn't appear that depth would be
			 * traversed... Adding logging statement before return to see if
			 * replacement classes are missed.
			 * 
			 */
			/*
			 * FIXME this traversal logic is all over the place, probably
			 * missing a lot more than we think, evaluate. Others appear to
			 * traverse depth via recursion
			 */
			/*
			 * If this node has a child, it's the next node to process.
			 * Otherwise, get the next sibling. If there is no next sibling,
			 * back up the tree to the parent.
			 */
			/*
			 * started hitting a bunch of not-ignored classes in the full xml
			 * traversal, so backing out and dealing with migrating PersonImpl
			 * and KualiCodeBase individually
			 */
			// TODO clean up commented out code
			// Node nextChild = childNode.getFirstChild();
			// if (nextChild == null) {
			// nextChild = childNode.getNextSibling();
			// }
			// if (nextChild == null) {
			// nextChild = childNode.getParentNode();
			// }
			Node nextChild = childNode.getNextSibling();
            transformClassNode(document, childNode);
            childNode = nextChild;
        }

		migratePersonObjects(document);
		migrateKualiCodeBaseObjects(document);

        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer trans = transFactory.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(document);
        trans.transform(source, result);
		/*
		 * (?m) puts the regex into multiline mode:
		 * https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.
		 * html#MULTILINE So the effect of this statement is
		 * "remove any empty lines"
		 */
        xml = writer.toString().replaceAll("(?m)^\\s+\\n", "");
		xml = xml + "<" + MAINTENANCE_ACTION_ELEMENT_NAME + ">" + maintenanceAction + "</"
				+ MAINTENANCE_ACTION_ELEMENT_NAME + ">";

		// TODO remove investigative logging
		for (String oldClassName : classNameRuleMap.keySet()) {
			if (xml.contains(oldClassName)) {
				LOGGER.warn("Document has classname in contents that should have been mapped: " + oldClassName);
			}
		}
		return xml;
    }


    /**
     * Upgrades the old Bo notes tag that was part of the maintainable to the new notes tag.
     *
     * @param oldXML - the xml to upgrade
     * @throws Exception
     */
    private String upgradeBONotes(String oldXML) throws Exception {
        // Get the old bo note xml
        String notesXml = StringUtils.substringBetween(oldXML, "<boNotes>", "</boNotes>");
        if (notesXml != null) {
			LOGGER.trace("BO Notes present, upgrading.");
            notesXml = notesXml.replace("org.kuali.rice.kns.bo.Note", "org.kuali.rice.krad.bo.Note");
            notesXml = "<org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl>\n"
                    + notesXml
                    + "\n</org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl>";
            
            int pos1 = oldXML.indexOf("<boNotes>");
            int pos2 = oldXML.indexOf("</boNotes>", pos1);
            
            if ((pos1 > -1) && (pos2 > pos1)) {
                oldXML = (oldXML.substring(0, pos1) + ">\n<notes>\n" + notesXml + "\n</notes>" + oldXML.substring(pos2 + "</boNotes>".length()));
            }
        }
        
        return oldXML;
    }

	/**
	 * Migrate any elements with the <code>class</code> containing
	 * <code>PersonImpl</code> from the provided {@link Document} if there is a
	 * mapping in {@link #classNameRuleMap}.
	 * 
	 * @param doc
	 */
    public void migratePersonObjects( Document doc ) {
		/*
		 * FIXME evaluate this method...
		 * class='org.kuali.rice.kim.impl.identity.PersonImpl' is a replacement
		 * class for class='org.kuali.rice.kim.bo.impl.PersonImpl', so there
		 * shouldn't be any instances of this 'new' class yet. Why is this
		 * method here? Was it some kind of cleanup in between runs while
		 * testing?
		 */
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression personProperties = null;
        try {
			String personImplClassName = null;
			for (String key : classNameRuleMap.keySet()) {
				if (key.endsWith("PersonImpl")) {
					personImplClassName = key;
				}
			}
			// if no mapping, nothing to do here
			if (personImplClassName == null) {
				return;
			}
			personProperties = xpath.compile("//*[@class='" + personImplClassName + "']");
            NodeList matchingNodes = (NodeList)personProperties.evaluate( doc, XPathConstants.NODESET );
            for(int i = 0; i < matchingNodes.getLength(); i++) {
                Node tempNode = matchingNodes.item(i);
				LOGGER.info("Migrating PersonImpl node: " + tempNode.getNodeName() + "/" + tempNode.getNodeValue());
				String newClassName = this.classNameRuleMap.get(personImplClassName);
				Node classAttr = tempNode.getAttributes().getNamedItem("class");
				classAttr.setNodeValue(newClassName);
            }
        } catch (XPathExpressionException e) {
			LOGGER.error("XPathException encountered: ", e);
        }
    }

	/**
	 * Migrate any elements with the <code>class</code> containing
	 * <code>KualiCodeBase</code> from the provided {@link Document} if there is
	 * a mapping in {@link #classNameRuleMap}.
	 * 
	 * @param doc
	 */
	public void migrateKualiCodeBaseObjects(Document doc) {
		/*
		 * FIXME evaluate this method...
		 * class='org.kuali.rice.kim.impl.identity.PersonImpl' is a replacement
		 * class for class='org.kuali.rice.kim.bo.impl.PersonImpl', so there
		 * shouldn't be any instances of this 'new' class yet. Why is this
		 * method here? Was it some kind of cleanup in between runs while
		 * testing?
		 */
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression personProperties = null;
		try {
			String kualiCodeBaseClassName = null;
			for (String key : classNameRuleMap.keySet()) {
				if (key.endsWith("KualiCodeBase")) {
					kualiCodeBaseClassName = key;
				}
			}
			// if no mapping, nothing to do here
			if (kualiCodeBaseClassName == null) {
				return;
			}
			personProperties = xpath.compile("//*[@class='" + kualiCodeBaseClassName + "']");
			NodeList matchingNodes = (NodeList) personProperties.evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < matchingNodes.getLength(); i++) {
				Node tempNode = matchingNodes.item(i);
				LOGGER.info("Migrating KualiCodeBase node: " + tempNode.getNodeName() + "/" + tempNode.getNodeValue());
				String newClassName = this.classNameRuleMap.get(kualiCodeBaseClassName);
				doc.renameNode(tempNode, null, newClassName);
			}
		} catch (XPathExpressionException e) {
			LOGGER.error("XPathException encountered: ", e);
		}
	}

	/**
	 * If a mapping for the <code>node</code> class exists in
	 * {@link #classNameRuleMap}, the given <code>node</code> is renamed. Then,
	 * if the <code>node</code> is a valid class, it is passed to the
	 * {@link #transformNode(Document, Node, Class, Map)} method first with the
	 * {@link #classPropertyRuleMap} value for the classname, then with the
	 * {@link #classPropertyRuleMap} value for "<code>*</code>".
	 * 
	 * @param document
	 *            Root level {@link Document}
	 * @param node
	 *            {@link Node} to transform
	 * @throws ClassNotFoundException
	 * @throws XPathExpressionException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
    private void transformClassNode(Document document, Node node) throws ClassNotFoundException, XPathExpressionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {

		String className = node.getNodeName();
		LOGGER.trace("Transforming class node for : " + node.getBaseURI() + "/" + className);
		if(this.classNameRuleMap.containsKey(className)) {
			String newClassName = this.classNameRuleMap.get(className);
			document.renameNode(node, null, newClassName);
			className = newClassName;
		}

		/*
		 * FIXME submethod transformNode recurses over children, this method
		 * does not, so only top-level class is transformed. Need to potentially
		 * transform classes of children. For now, handling by traversing tree
		 * in calling method, however this has the unintended side effect of
		 * recursing over each element in this submethod as well....
		 */
        if (isValidClass(className)) {
            Class<?> dataObjectClass = Class.forName(className);

            if(classPropertyRuleMap.containsKey(className)) {
                transformNode(document, node, dataObjectClass, classPropertyRuleMap.get(className));
            }

            transformNode(document, node, dataObjectClass, classPropertyRuleMap.get("*"));
        }
	}

	/**
	 * Does the following:
	 * <ol>
	 * <li>Recursively calls this method on all child elements of
	 * <code>talist</code> to handle any child lists first
	 * <li>Remove the attributes {@link #SERIALIZATION_ATTRIBUTE} and
	 * {@link #CLASS_ATTRIBUTE} of <code>talist</code></li>
	 * <li>If
	 * <code>//[talist.getNodeName()]/org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl/default/size/</code>
	 * evaluates to a value >1, indicating elements in this list, call
	 * {@link #transformClassNode(Document, Node)} on that element and store to
	 * readd</li>
	 * <li>Remove all child elements of <code>talist</code></li>
	 * <li>Readd list elements calculated and transformed above</li>
	 * </ol>
	 * 
	 * @param document
	 *            Root {@link Document}
	 * @param xpath
	 *            {@link XPath} to use during evaluation
	 * @param talist
	 *            {@link Element} to process typed array lists on
	 * @throws XPathExpressionException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
    private void handleTypedArrayList(Document document, XPath xpath , Element talist) throws XPathExpressionException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        NodeList nodeList = talist.getElementsByTagName(talist.getNodeName());
		LOGGER.trace("Handling typed array list: " + talist.getBaseURI());
        // handle any child lists first
        for (int i = 0; i < nodeList.getLength(); ++i) {
            handleTypedArrayList(document, xpath , (Element)nodeList.item(i));
        }
        
        talist.removeAttribute(SERIALIZATION_ATTRIBUTE);
        talist.removeAttribute(CLASS_ATTRIBUTE);
        XPathExpression listSizeExpression = xpath.compile("//" + talist.getNodeName() + "/org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl/default/size/text()");
        String size = (String)listSizeExpression.evaluate(talist, XPathConstants.STRING);
        List<Node> nodesToAdd = new ArrayList<Node>();
        if(StringUtils.isNotBlank(size) && Integer.valueOf(size) > 0) {
            XPathExpression listTypeExpression = xpath.compile("//" + talist.getNodeName() + "/org.kuali.rice.kns.util.TypedArrayList/default/listObjectType/text()");
            String listType = (String)listTypeExpression.evaluate(talist, XPathConstants.STRING);
            XPathExpression listContentsExpression = xpath.compile("//" + talist.getNodeName() + "/org.apache.ojb.broker.core.proxy.ListProxyDefaultImpl/" + listType);
            NodeList listContents = (NodeList)listContentsExpression.evaluate(talist, XPathConstants.NODESET);
            for(int i = 0; i < listContents.getLength(); i++) {
                Node tempNode = listContents.item(i);
                transformClassNode(document, tempNode);
                nodesToAdd.add(tempNode);
            }
        }
        for(Node removeNode = talist.getFirstChild(); removeNode != null;) {
            Node nextRemoveNode = removeNode.getNextSibling();
            talist.removeChild(removeNode);
            removeNode = nextRemoveNode;
        }
        for(Node nodeToAdd : nodesToAdd) {
            talist.appendChild(nodeToAdd);
        }
    }
    
	/**
	 * For each child of <code>node</code>
	 * 
	 * @param document
	 * @param node
	 * @param currentClass
	 * @param propertyMappings
	 * @throws ClassNotFoundException
	 * @throws XPathExpressionException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
	private void transformNode(Document document, Node node, Class<?> currentClass, Map<String, String> propertyMappings) throws ClassNotFoundException, XPathExpressionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		LOGGER.trace("Transforming node: " + node.getBaseURI() + "/" + node.getNodeName());
		for(Node childNode = node.getFirstChild(); childNode != null;) {
			Node nextChild = childNode.getNextSibling();
			String propertyName = childNode.getNodeName();
			if(childNode.hasAttributes()) {
				XPath xpath = XPathFactory.newInstance().newXPath();
				Node serializationAttribute = childNode.getAttributes().getNamedItem(SERIALIZATION_ATTRIBUTE);
				if(serializationAttribute != null && StringUtils.equals(serializationAttribute.getNodeValue(), "custom")) {
					Node classAttribute = childNode.getAttributes().getNamedItem(CLASS_ATTRIBUTE);
					if(classAttribute != null && StringUtils.equals(classAttribute.getNodeValue(), "org.kuali.rice.kns.util.TypedArrayList")) {
                        handleTypedArrayList(document, xpath, (Element)childNode);
					} else {
						((Element)childNode).removeAttribute(SERIALIZATION_ATTRIBUTE);
						
						XPathExpression mapContentsExpression = xpath.compile("//" + propertyName + "/map/string");
						NodeList mapContents = (NodeList)mapContentsExpression.evaluate(childNode, XPathConstants.NODESET);
						List<Node> nodesToAdd = new ArrayList<Node>();
						if(mapContents.getLength() > 0 && mapContents.getLength() % 2 == 0) {
							for(int i = 0; i < mapContents.getLength(); i++) {
								Node keyNode = mapContents.item(i);
								Node valueNode = mapContents.item(++i);
								Node entryNode = document.createElement("entry");
								entryNode.appendChild(keyNode);
								entryNode.appendChild(valueNode);
								nodesToAdd.add(entryNode);
							}
						}
						for(Node removeNode = childNode.getFirstChild(); removeNode != null;) {
							Node nextRemoveNode = removeNode.getNextSibling();
							childNode.removeChild(removeNode);
							removeNode = nextRemoveNode;
						}
						for(Node nodeToAdd : nodesToAdd) {
							childNode.appendChild(nodeToAdd);
						}
					}
				}
			}
			if(propertyMappings != null && propertyMappings.containsKey(propertyName)) {
				String newPropertyName = propertyMappings.get(propertyName);
				if(StringUtils.isNotBlank(newPropertyName)) {
					document.renameNode(childNode, null, newPropertyName);
					propertyName = newPropertyName;
				} else {
					// If there is no replacement name then the element needs
					// to be removed and skip all other processing
					node.removeChild(childNode);
					childNode = nextChild;
					continue;
				}
			}

            if(dateRuleMap != null && dateRuleMap.containsKey(propertyName)) {
                String newDateValue = dateRuleMap.get(propertyName);
                if(StringUtils.isNotBlank(newDateValue)) {
                    if ( childNode.getTextContent().length() == 10 ) {
                        childNode.setTextContent( childNode.getTextContent() + " " + newDateValue );

                    }
                }
            }

            if ((currentClass != null) && isValidClass(currentClass)) {
                if (childNode.hasChildNodes() && !(Collection.class.isAssignableFrom(currentClass) || Map.class
                        .isAssignableFrom(currentClass))) {
                    Class<?> propertyClass = PropertyUtils.getPropertyType(currentClass.newInstance(), propertyName);
                    if (propertyClass != null && classPropertyRuleMap.containsKey(propertyClass.getName())) {
                        transformNode(document, childNode, propertyClass, this.classPropertyRuleMap.get(
                                propertyClass.getName()));
                    }
                    
                    transformNode(document, childNode, propertyClass, classPropertyRuleMap.get("*"));
                }
            }
			childNode = nextChild;
		}
	}
    
	/**
	 * Storefront to call {@link #isValidClass(String)} with
	 * {@link Class#getName()}
	 * 
	 * @param c
	 * @return value of {@link #isValidClass(String)} with
	 *         {@link Class#getName()}
	 */
	private boolean isValidClass(Class<?> c) {
        return isValidClass(c.getName());
    }

	/**
	 * @param className
	 *            {@link String} of a classname to check the validity of
	 * @return <code>true</code> if <code>className</code> does NOT start with
	 *         <code>edu.arizona</code> or <code>com.rsmart.</code>, and
	 *         <code>className</code> is not in the {@link #ignoreClassSet}.
	 */
    private boolean isValidClass(String className) {
        if (className.startsWith("edu.arizona") || className.startsWith("com.rsmart.")) {
            if (!uaMaintenanceDocClasses.contains(className)) {
                uaMaintenanceDocClasses.add(className);
				LOGGER.info("non-kuali maintenance document class ignored - " + className);
            }
            return false;
        } else {
            return !ignoreClassSet.contains(className);
        }
    }
    /**
     * Reads the rule xml and sets up the rule maps that will be used to transform the xml
     */
	private void setRuleMaps() throws Exception {
		setupConfigurationMaps();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(rulesXmlFile);

        doc.getDocumentElement().normalize();
        XPath xpath = XPathFactory.newInstance().newXPath();

        // Get the moved classes rules
        XPathExpression exprClassNames = xpath.compile("//*[@name='maint_doc_classname_changes']/pattern");
        NodeList classNamesList = (NodeList) exprClassNames.evaluate(doc, XPathConstants.NODESET);
        for (int s = 0; s < classNamesList.getLength(); s++) {
            String matchText = xpath.evaluate("match/text()", classNamesList.item(s));
            String replaceText = xpath.evaluate("replacement/text()", classNamesList.item(s));
            classNameRuleMap.put(matchText, replaceText);
        }

        // Get the property changed rules

        XPathExpression exprClassProperties = xpath.compile(
                "//*[@name='maint_doc_changed_class_properties']/pattern");
        XPathExpression exprClassPropertiesPatterns = xpath.compile("pattern");
        NodeList propertyClassList = (NodeList) exprClassProperties.evaluate(doc, XPathConstants.NODESET);
        for (int s = 0; s < propertyClassList.getLength(); s++) {
            String classText = xpath.evaluate("class/text()", propertyClassList.item(s));
            Map<String, String> propertyRuleMap = new HashMap<String, String>();
            NodeList classPropertiesPatterns = (NodeList) exprClassPropertiesPatterns.evaluate(
                    propertyClassList.item(s), XPathConstants.NODESET);
            for (int c = 0; c < classPropertiesPatterns.getLength(); c++) {
                String matchText = xpath.evaluate("match/text()", classPropertiesPatterns.item(c));
                String replaceText = xpath.evaluate("replacement/text()", classPropertiesPatterns.item(c));
                propertyRuleMap.put(matchText, replaceText);
            }
            classPropertyRuleMap.put(classText, propertyRuleMap);
        }

        // Get the Date rules
        XPathExpression dateFieldNames = xpath.compile("//*[@name='maint_doc_date_changes']/pattern");
        NodeList DateNamesList = (NodeList) dateFieldNames.evaluate(doc, XPathConstants.NODESET);
        for (int s = 0; s < DateNamesList.getLength(); s++) {
            String matchText = xpath.evaluate("match/text()", DateNamesList.item(s));
            String replaceText = xpath.evaluate("replacement/text()", DateNamesList.item(s));
            dateRuleMap.put(matchText, replaceText);
        }
	}

	/**
	 * Constructs the various instance variable maps and pre-populates
	 * {@link #classPropertyRuleMap} with some values which apply to all BOs.
	 */
	private void setupConfigurationMaps() {
		classNameRuleMap = new HashMap<String, String>();
		classPropertyRuleMap = new HashMap<String, Map<String,String>>();
        dateRuleMap = new HashMap<String, String>();

        // Pre-populate the class property rules with some defaults which apply to every BO
		Map<String, String> defaultPropertyRules = new HashMap<String, String>();
		defaultPropertyRules.put("boNotes", "");
		defaultPropertyRules.put("autoIncrementSet", "");
        classPropertyRuleMap.put("*", defaultPropertyRules);
	}
}
