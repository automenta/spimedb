package spimedb.ui;

//import it.unimi.dsi.fastutil.longs.Long2ReferenceRBTreeMap;

import jcog.Str;
import jcog.data.list.Lst;
import jcog.data.map.UnifriedMap;
import jcog.io.bzip2.BZip2InputStream;
import jcog.tree.rtree.rect.RectF;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static java.lang.Double.parseDouble;
import static jcog.Str.n4;

/**
 * Created by unkei on 2017/04/25.
 */
public class Osm {

    /** lon,lat coordinates */
    public RectF geoBounds;

    public final LongObjectHashMap<OsmNode> nodes = new LongObjectHashMap<>();
//    public final LongObjectHashMap<OsmNode> nodes;
    public final LongObjectHashMap<OsmRelation> relations = new LongObjectHashMap<>();
    public final LongObjectHashMap<OsmWay> ways = new LongObjectHashMap<>();


    public String id;

    /** TODO abstract
     * bzless ~/test.osm.bz2  | fgrep 'k=' | sort > /tmp/x

     * */
    private static final Set<String> filteredKeys = Set.of(
            "source", "tiger:cfcc", "odbl"
    );

    public Osm() {

    }

    /** absorb all the content from a sub-map */
    public final void addAll(Osm x) {
        nodes.putAll(x.nodes);
        relations.putAll(x.relations);
        ways.putAll(x.ways);
        geoBounds = this.geoBounds == null ? x.geoBounds : geoBounds.mbr(x.geoBounds);
    }


    @Override
    public String toString() {
        return geoBounds + " (" + nodes.size() + " nodes, " + relations.size() + " relations, " + ways.size() + " ways)";
    }

    public Osm load(String filename) throws SAXException, IOException {
        InputStream fis = new FileInputStream(filename);

        if (filename.endsWith(".gz")) {
            fis = new GZIPInputStream(fis);
        } else if (filename.endsWith(".bz2")) {
            fis = new BZip2InputStream(fis);
        }

        return load(fis);
    }

    public static URL url(String apiURL, double lonMin, double latMin, double lonMax, double latMax)  {
        try {
            return new URL(apiURL + "/api/0.6/map?bbox=" +
                    Str.n4(lonMin) + ',' + Str.n4(latMin) + ',' +
                    Str.n4(lonMax) + ',' + Str.n4(latMax) );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(String apiURL, double lonMin, double latMin, double lonMax, double latMax) throws SAXException, IOException {
        load(url(apiURL, lonMin, latMin, lonMax, latMax).openStream());
    }

    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder documentBuilder;
    {
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setCoalescing(true);
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    /** TODO make static so it can customize/optimize the returned instance */
    public Osm load(InputStream fis) throws SAXException, IOException {

        Document document = documentBuilder.parse(fis);


        Collection<Element> relationElements = new Lst<>();

        NodeList childNodes = document.getDocumentElement().getChildNodes();
        int cl = childNodes.getLength();
        for (int i = 0; i < cl; i++) {
            Node n = childNodes.item(i);

            switch (n.getNodeName()) {
                case "bounds" -> {
                    Element e = ((Element) n);
                    assert (this.geoBounds == null);
                    this.geoBounds = RectF.XYXY(
                            (float) parseDouble(e.getAttribute("minlon")),
                            (float) parseDouble(e.getAttribute("minlat")),
                            (float) parseDouble(e.getAttribute("maxlon")),
                            (float) parseDouble(e.getAttribute("maxlat"))
                    );
                }
                case "node" -> {
                    Element childElement = (Element) n;
                    long id = Str.l(childElement.getAttribute("id"));

                    Map<String, String> osmTags = null;
                    NodeList nodeChildren = childElement.getElementsByTagName("tag") /*childNode.getChildNodes()*/;
                    int nnc = nodeChildren.getLength();
                    for (int j = 0; j < nnc; j++) {
                        Node nodeChild = nodeChildren.item(j);
                        /*if ("tag".equals(nodeChild.getNodeName()))*/
                        {
                            Element ne = (Element) nodeChild;
                            String key = ne.getAttribute("k");
                            if (!filteredKeys.contains(key)) {
                                if (osmTags == null)
                                    osmTags = new UnifriedMap<>(1);
                                osmTags.put(key, ne.getAttribute("v"));
                            }
                        }
                    }


                    this.nodes.put(id, new OsmNode(
                            id, new GeoVec3(childElement), osmTags));


                }
                case "way" -> {
                    Element childElement = (Element) n;
                    long id = Str.l(childElement.getAttribute("id"));

                    List<OsmElement> refOsmNodes = new Lst<>();
                    Map<String, String> osmTags = null;

                    NodeList wayChildren = n.getChildNodes();
                    int l = wayChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node wayChild = wayChildren.item(j);
                        String node = wayChild.getNodeName();
                        switch (node) {
                            case "nd" -> refOsmNodes.add(
                                    this.nodes.get(Str.l(((Element) wayChild).getAttribute("ref")))
                            );
                            case "tag" -> {
                                Element nodeChildElement = (Element) wayChild;
                                if (osmTags == null)
                                    osmTags = new UnifriedMap<>(1);
                                osmTags.put(nodeChildElement.getAttribute("k"), nodeChildElement.getAttribute("v"));
                            }
                        }
                    }

                    this.ways.put(id, new OsmWay(id, refOsmNodes, osmTags));


                }
                case "relation" -> {
                    Element childElement = (Element) n;
                    long id = Str.l(childElement.getAttribute("id"));
                    NodeList relationChildren = childElement.getElementsByTagName("tag");
                    Map<String, String> osmTags = null;
                    int l = relationChildren.getLength();
                    for (int j = 0; j < l; j++) {
                        Node relationChild = relationChildren.item(j);
                        /*if ("tag".equals(relationChild.getNodeName()))*/
                        {
                            if (osmTags == null)
                                osmTags = new UnifriedMap<>(1);
                            Element e = (Element) relationChild;
                            osmTags.put(e.getAttribute("k"), e.getAttribute("v"));
                        }
                    }
                    this.relations.put(id, new OsmRelation(id, null, osmTags));
                    relationElements.add(childElement);
                }
            }
        }


        
        for (Element relationElement : relationElements) {
            long id = Str.l(relationElement.getAttribute("id"));



            //Map<String, String> tags = osmRelation.tags;
//            String highway, natural, building, building_part, landuse;
//            if (tags.isEmpty()) {
//                highway = natural = building = building_part = landuse = null;
//            } else {
//                highway = tags.get("highway");
//                natural = tags.get("natural");
//                building = tags.get("building");
//                building_part = tags.get("building:part");
//                landuse = tags.get("landuse");
//            }

            NodeList relationChildren = relationElement.getElementsByTagName("member"); 
            List<OsmElement> osmMembers = null;
            int l = relationChildren.getLength();
            for (int j = 0; j < l; j++) {
                Node relationChild = relationChildren.item(j);
                /*if ("member".equals(relationChild.getNodeName()))*/
                {
                    Element r = (Element) relationChild;
                    String type = r.getAttribute("type");
                    long ref = Str.l(r.getAttribute("ref"));

                    //                            if (member != null) {
//                                if (highway != null) {
//                                    member.tag("highway", highway);
//                                }
//                                if (natural != null) {
//                                    member.tag("natural", natural);
//                                }
//                                if (building != null) {
//                                    member.tag("building", building);
//                                }
//                                if (building_part != null) {
//                                    member.tag("building:part", building_part);
//                                }
//                                if (landuse != null) {
//                                    member.tag("landuse", landuse);
//                                }
                    //                            }
                    OsmElement member = switch (type) {
                        case "node" -> this.nodes.get(ref);
                        case "way" -> this.ways.get(ref);
                        case "relation" -> this.relations.get(ref);
                        default -> null;
                    };
                    if (member != null) {
                        if (osmMembers == null)
                            osmMembers = new Lst<>(1);
                        osmMembers.add(member);
                    }
                }
            }

            if (osmMembers != null && !osmMembers.isEmpty())
                this.relations.get(id).addChildren(osmMembers);
        }

        
        
        for (Element relationElement : relationElements) {
            long id = Str.l(relationElement.getAttribute("id"));

            OsmRelation osmRelation = this.relations.get(id);

            Map<String, String> tags = osmRelation.tags;
            String type = tags.get("type");

            if (!"multipolygon".equals(type))
                continue;

            List<? extends OsmElement> oc = osmRelation.children;
            for (int i = 0, ocSize = oc.size(); i < ocSize; i++) {
                OsmElement e1 = oc.get(i);

                if (!(e1 instanceof OsmWay w1))
                    return this;

                if (w1.isLoop())
                    return this;

                ListIterator<? extends OsmElement> ii = oc.listIterator(i);
                while (ii.hasNext()) {
                    OsmElement e2 = ii.next();
                    if (!(e2 instanceof OsmWay w2))
                        continue;

                    if (w1.isFollowedBy(w2)) {
                        w1.addOsmWay(w2);
                        ii.remove();
                    }
                }
            }
        }

        return this;
    }


    public boolean isEmpty() {
        return this.nodes.isEmpty() && this.ways.isEmpty() && this.relations.isEmpty();
    }

    public void clear() {
        nodes.clear();
        ways.clear();
        relations.clear();
    }

    public void print() {
        printOsmNodes(nodes.values());
        printOsmWays(ways);
        printOsmRelations(relations);
    }

    private static void printTags(Map<String, String> tags, int indent) {
        for (Map.Entry<String, String> stringStringEntry : tags.entrySet()) {
            String v = stringStringEntry.getValue();
            printIndent(indent);
            System.out.printf("%s=%s\n", stringStringEntry.getKey(), v);
        }
    }

    private static void printOsmNodes(Iterable<? extends OsmElement> osmNodes) {
        printOsmNodes(osmNodes, 0);
    }

    private static void printOsmNodes(Iterable<? extends OsmElement> osmNodes, int indent) {
        for (OsmElement osmNode : osmNodes) {
            printOsmNode((OsmNode) osmNode, indent);
        }
    }

    private static void printOsmNode(OsmNode osmNode, int indent) {
        GeoVec3 pos = osmNode.pos;
        printIndent(indent);
        System.out.printf("<node id=%s, lat=%f, lon=%f>\n", osmNode.id, pos.getLatitude(), pos.getLongitude());
        printTags(osmNode.tags, indent + 1);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays) {
        printOsmWays(osmWays, 0);
    }

    private static void printOsmWays(Iterable<OsmWay> osmWays, int indent) {
        for (OsmWay osmWay : osmWays) {
            printOsmWay(osmWay, indent);
        }
    }

    private static void printOsmWay(OsmWay osmWay, int indent) {
        printIndent(indent);
        System.out.printf("<way id=%s>\n", osmWay.id);
        printOsmNodes(osmWay.children, indent + 1);
        printTags(osmWay.tags, indent + 1);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations) {
        printOsmRelations(osmRelations, 0);
    }

    private static void printOsmRelations(Iterable<OsmRelation> osmRelations, int indent) {
        for (OsmRelation osmRelation : osmRelations) {
            printOsmRelation(osmRelation, indent);
        }
    }

    private static void printOsmRelation(OsmRelation osmRelation, int indent) {
        printIndent(indent);
        System.out.printf("<relation id=%s>\n", osmRelation.id);
        printOsmElements(osmRelation.children, indent + 1);
        printTags(osmRelation.tags, indent + 1);
    }

    private static void printOsmElements(Iterable<? extends OsmElement> osmElements, int indent) {
        for (OsmElement osmElement : osmElements) {
            if (osmElement.getClass() == OsmNode.class) {
                printOsmNode((OsmNode) osmElement, indent);
            } else if (osmElement.getClass() == OsmWay.class) {
                printOsmWay((OsmWay) osmElement, indent);
            } else if (osmElement.getClass() == OsmRelation.class) {
                printOsmRelation((OsmRelation) osmElement, indent);
            }
        }
    }

    private static void printIndent(int indent) {
        for (int i = 0; i < indent; i++)
            System.out.print("  ");
    }

    private static void printNode(int indent, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
        System.out.print(node.getNodeName());

        NamedNodeMap nodeMap = node.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node attr = nodeMap.item(i);
            System.out.print(", " + attr.getNodeName() + '=' + attr.getNodeValue());
        }
        System.out.println();

        NodeList childNodes = node.getChildNodes();
        int l = childNodes.getLength();
        for (int i = 0; i < l; i++) {
            printNode(indent + 1, childNodes.item(i));
        }
    }

}