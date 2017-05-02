package spimedb.media.kml;

import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.evt.EventReaderImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.Namespace;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Element;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.input.GISInputStreamBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public abstract class XmlInputStream extends GISInputStreamBase {
    private static final Logger log = LoggerFactory.getLogger(XmlInputStream.class);

    protected InputStream is;
    protected EventReaderImpl stream;

    @NonNull
    private final String encoding = "UTF-8";

    public XmlInputStream(InputStream inputStream) throws IOException {

        if(inputStream == null) {
            throw new IllegalArgumentException("inputStream should never be null");
        } else {
            this.is = inputStream;

            try {

                AsyncXMLInputFactory f = new InputFactoryImpl();
                f.configureForSpeed();

                Reader reader = new InputStreamReader(is,encoding);

                stream = (EventReaderImpl) f.createXMLEventReader(reader);


                //this.stream = ms_fact.createXMLEventReader(this.is);

            } catch (XMLStreamException var3) {
                throw new IOException(var3);
            }
        }
    }

    public XmlInputStream(InputStream inputStream, DocumentType type) throws IOException {
        this(inputStream);
        DocumentStart ds = new DocumentStart(type);
        this.addLast(ds);
    }

    public void close() {
        if(this.stream != null) {
            try {
                this.stream.close();
            } catch (XMLStreamException var2) {
                log.warn("Failed to close reader", var2);
            }
        }

        if(this.is != null) {
            IOUtils.closeQuietly(this.is);
            this.is = null;
        }

    }

    @NonNull
    public String getEncoding() {
        return this.encoding;
    }

//    protected void setEncoding(String encoding) {
//        if(StringUtils.isNotBlank(encoding)) {
//            log.debug("set encoding={}", encoding);
//            this.encoding = encoding;
//        } else {
//            log.debug("null/empty encoding. assuming UTF-8");
//        }
//
//    }

    protected static boolean foundEndTag(XMLEvent event, String tag) {
        return event == null || event.getEventType() == 2 && event.asEndElement().getName().getLocalPart().equals(tag);
    }

    protected static boolean foundEndTag(XMLEvent event, QName name) {
        return event == null || event.getEventType() == 2 && event.asEndElement().getName().equals(name);
    }

    protected static boolean foundStartTag(StartElement se, String tag) {
        return se.getName().getLocalPart().equals(tag);
    }

    @NonNull
    protected IGISObject getForeignElement(StartElement se) throws XMLStreamException {
        Element el = new Element();
        QName qName = se.getName();
        el.setName(qName.getLocalPart());
        String nsURI = qName.getNamespaceURI();
        if(StringUtils.isNotBlank(nsURI)) {
            try {
                el.setNamespace(Namespace.getNamespace(qName.getPrefix(), nsURI));
            } catch (IllegalArgumentException var10) {
                log.error("Failed to assign namespace {}", qName);
            }
        }

        Iterator nsiter = se.getNamespaces();

        while(nsiter.hasNext()) {
            javax.xml.stream.events.Namespace aiter = (javax.xml.stream.events.Namespace)nsiter.next();
            String nextel = aiter.getPrefix();
            if(StringUtils.isNotBlank(nextel)) {
                el.addNamespace(Namespace.getNamespace(nextel, aiter.getNamespaceURI()));
            }
        }

        String text;
        String oldText;
        Attribute nextel1;
        for(Iterator aiter1 = se.getAttributes(); aiter1.hasNext(); el.getAttributes().put(text, nextel1.getValue())) {
            nextel1 = (Attribute)aiter1.next();
            oldText = nextel1.getName().getPrefix();
            if(StringUtils.isBlank(oldText)) {
                text = nextel1.getName().getLocalPart();
            } else {
                text = oldText + ':' + nextel1.getName().getLocalPart();
            }
        }

        XMLEvent nextel2 = this.stream.nextEvent();

        while(true) {
            if(nextel2 instanceof Characters) {
                Characters text1 = (Characters)nextel2;
                oldText = el.getText();
                if(oldText != null) {
                    el.setText(oldText + text1.getData());
                } else {
                    el.setText(text1.getData());
                }
            } else if(nextel2.isStartElement()) {
                el.getChildren().add((Element)this.getForeignElement(nextel2.asStartElement()));
            } else if(nextel2.isEndElement()) {
                return el;
            }

            nextel2 = this.stream.nextEvent();
        }
    }

    @NonNull
    protected String getSerializedElement(StartElement start) throws XMLStreamException {
        Element el = (Element)this.getForeignElement(start);
        StringBuilder sb = new StringBuilder(100);

        for (Object child : el.getChildren()) {
            this.serialize((Element)child, sb);
        }

        sb.append(el.getText());
        return sb.toString();
    }

    private void serialize(Element el, StringBuilder sb) {
        String name = StringUtils.isNotBlank(el.getPrefix())?el.getPrefix() + ':' + el.getName():el.getName();
        sb.append('<');
        sb.append(name);
        Iterator i$ = el.getAttributes().entrySet().iterator();

        while(i$.hasNext()) {
            Map.Entry child = (Map.Entry)i$.next();
            sb.append(' ');
            sb.append((String)child.getKey());
            sb.append('=');
            sb.append('\"');
            sb.append((String)child.getValue());
            sb.append('\"');
        }

        sb.append('>');
        i$ = el.getChildren().iterator();

        while(i$.hasNext()) {
            Element child1 = (Element)i$.next();
            this.serialize(child1, sb);
        }

        sb.append(el.getText());
        sb.append("</");
        sb.append(name);
        sb.append('>');
    }

    /*@Nullable*/
    protected String getNonEmptyElementText() throws XMLStreamException {
        String elementText = this.stream.getElementText();
        if(elementText != null && !elementText.isEmpty()) {
            elementText = elementText.trim();
            return elementText.isEmpty()?null:elementText;
        } else {
            return null;
        }
    }

    /*@Nullable*/
    protected Integer getIntegerElementValue(String localName) throws XMLStreamException {
        String elementText = this.getNonEmptyElementText();
        if(elementText != null) {
            try {
                return Integer.parseInt(elementText);
            } catch (NumberFormatException var4) {
                log.warn("Ignoring bad value for {}: {}", localName, var4);
            }
        }

        return null;
    }

    /*@Nullable*/
    protected Double getDoubleElementValue(String localName) throws XMLStreamException {
        String elementText = this.stream.getElementText();
        if(elementText != null && StringUtils.isNotBlank(elementText)) {
            try {
                return Double.parseDouble(elementText);
            } catch (NumberFormatException var4) {
                log.warn("Ignoring bad value for {}: {}", localName, var4);
            }
        }

        return null;
    }

    /*@Nullable*/
    protected String getElementText(QName name) throws XMLStreamException {
        try {
            return this.getNonEmptyElementText();
        } catch (XMLStreamException var3) {
            log.warn("Unable to parse {} as text element: {}", name.getLocalPart(), var3);
            XmlInputStream.skipNextElement(this.stream, name);
            return null;
        }
    }

    protected static void skipNextElement(XMLEventReader element, QName name) throws XMLStreamException {
        XMLEvent next;
        do {
            next = element.nextEvent();
        } while(next != null && !XmlInputStream.foundEndTag(next, name));

    }
}
