package com.kixeye.kixmpp.jdom;

/*
 * #%L
 * KIXMPP Parent
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.codehaus.stax2.XMLStreamReader2;
import org.jdom2.Attribute;
import org.jdom2.AttributeType;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.JDOMFactory;
import org.jdom2.Namespace;
import org.jdom2.input.stax.DTDParser;

/**
 * Builds a JDOM {@link Document} from a StAX Stream. This class is NOT thread safe.
 * 
 * @author ebahtijaragic
 */
public class StAXElementBuilder {
	private JDOMFactory builderFactory = new DefaultJDOMFactory();
	
	private final boolean ignoreInvalidNamespaces;

	private Element rootElement;
	
	private Element currentElement;
	
	/**
	 * @param ignoreInvalidNamespaces
	 */
	public StAXElementBuilder(boolean ignoreInvalidNamespaces) {
		this.ignoreInvalidNamespaces = ignoreInvalidNamespaces;
	}

	/**
	 * Processes the current event on a stream reader.
	 * 
	 * @param streamReader
	 * @throws JDOMException 
	 */
	public void process(XMLStreamReader2 streamReader) throws JDOMException {
		switch (streamReader.getEventType()) {
			case XMLStreamReader2.START_ELEMENT:
			{
				// build the element
				Namespace namespace = getNamespace(streamReader.getPrefix(), streamReader.getNamespaceURI());
				
				Element element = null;
				if (namespace == null) {
					element = builderFactory.element(streamReader.getLocalName());
				} else {
					element = builderFactory.element(streamReader.getLocalName(), getNamespace(streamReader.getPrefix(), streamReader.getNamespaceURI()));
				}

				if (rootElement == null) {
					rootElement = element;
					currentElement = element;
				} else {
					currentElement.addContent(element);
					currentElement = element;
				}

				// set attributes
				for (int i = 0, len = streamReader.getAttributeCount(); i<len ; i++) {
					namespace = getNamespace(streamReader.getAttributePrefix(i), streamReader.getAttributeNamespace(i));
					Attribute attribute = null;
					
					if (namespace == null) {
						attribute = builderFactory.attribute(
								streamReader.getAttributeLocalName(i),
								streamReader.getAttributeValue(i), 
								AttributeType.getAttributeType(streamReader.getAttributeType(i)),
								getNamespace(streamReader.getAttributePrefix(i), streamReader.getAttributeNamespace(i)));
					} else {
						attribute = builderFactory.attribute(
								streamReader.getAttributeLocalName(i),
								streamReader.getAttributeValue(i), 
								AttributeType.getAttributeType(streamReader.getAttributeType(i)));
					}
					
					builderFactory.setAttribute(element, attribute);
				}
				
				// set namespaces
				for (int i = 0, len = streamReader.getNamespaceCount(); i < len; i++) {
					namespace = getNamespace(streamReader.getNamespacePrefix(i), streamReader.getNamespaceURI(i));
					
					if (namespace != null) {
						element.addNamespaceDeclaration(namespace);
					}
				}
			}
			break;
			case XMLStreamReader2.END_ELEMENT:
			{
				if (currentElement != null) {
					currentElement = currentElement.getParentElement();
					
					if (currentElement == null) {
						currentElement = rootElement;
					}
				}
			}
			break;
			case XMLStreamReader2.SPACE:
			case XMLStreamReader2.CHARACTERS:
			{
				currentElement.addContent(builderFactory.text(streamReader.getText()));
			}
			break;
			case XMLStreamReader2.CDATA:
			{
				currentElement.addContent(builderFactory.cdata(streamReader.getText()));
			}
			break;
			case XMLStreamReader2.ENTITY_REFERENCE:
			{
				currentElement.addContent(builderFactory.entityRef(streamReader.getText()));
			}
			break;
			case XMLStreamReader2.COMMENT:
			{
				currentElement.addContent(builderFactory.comment(streamReader.getText()));
			}
			break;
			case XMLStreamReader2.PROCESSING_INSTRUCTION:
			{
				currentElement.addContent(builderFactory.processingInstruction(streamReader.getPITarget(), streamReader.getPIData()));
			}
			break;
			case XMLStreamReader2.DTD:
			{
				currentElement.addContent(DTDParser.parse(streamReader.getText(), builderFactory));
			}
			break;
		}
	}
	
	/**
	 * Gets the namespace.
	 * 
	 * @param prefix
	 * @param uri
	 * @return
	 */
	private Namespace getNamespace(String prefix, String uri) {
		Namespace namespace = null;
		
		try {
			namespace = Namespace.getNamespace(prefix, uri);
		} catch (Exception e) {
			if (!ignoreInvalidNamespaces) {
				throw e;
			}
		}
		
		return namespace;
	}
	
	/**
	 * Gets the element in the current state.
	 * 
	 * @return
	 */
	public Element getElement() {
		return rootElement;
	}
}
