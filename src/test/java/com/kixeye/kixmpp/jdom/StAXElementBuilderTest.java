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

import java.nio.charset.StandardCharsets;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.aalto.AsyncInputFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.kixeye.kixmpp.jdom.StAXElementBuilder;

/**
 * Tests the {@link StAXElementBuilder}
 * 
 * @author ebahtijaragic
 */
public class StAXElementBuilderTest {
	@Test
	public void testFullXml() throws Exception {
		InputFactoryImpl inputFactory = new InputFactoryImpl();
		AsyncXMLStreamReader streamReader = inputFactory.createAsyncXMLStreamReader();
		AsyncInputFeeder asyncInputFeeder = streamReader.getInputFeeder();
		
		byte[] xmlData = 
				(	"<?xml version='1.0'?>"
				+	"<someRootElement someAttribute='someAttributeValue' xmlns='http://someDomain.com/somePath'>"
				+		"<!-- Some Comment -->"
				+ 		"<someChildElement someChildElementAttribute='someChildElementAttributeValue'/>"
				+ 	"</someRootElement>").getBytes(StandardCharsets.UTF_8);
		
		asyncInputFeeder.feedInput(xmlData, 0, xmlData.length);
		
		StAXElementBuilder elementBuilder = new StAXElementBuilder(true);
		
		int event = -1;
		int elementsActive = 0;
		
		while ((event = streamReader.next()) > 0) {
			elementsActive += (event == 1 ? 1 : 0);
			elementsActive -= (event == 2 ? 1 : 0);

			elementBuilder.process(streamReader);
			
			if (event != 7 && elementsActive < 1) {
				break;
			}
		}
		
		Element someRootElement = elementBuilder.getElement();
		
		Assert.assertNotNull(someRootElement);
		Assert.assertEquals("someRootElement", someRootElement.getName());
		Assert.assertEquals("someAttributeValue", someRootElement.getAttributeValue("someAttribute"));
		Assert.assertEquals("http://someDomain.com/somePath", someRootElement.getNamespaceURI());
		
		Element someChildElement = someRootElement.getChildren().get(0);

		Assert.assertNotNull(someChildElement);
		Assert.assertEquals("someChildElement", someChildElement.getName());
		Assert.assertEquals("someChildElementAttributeValue", someChildElement.getAttributeValue("someChildElementAttribute"));
	}
}
