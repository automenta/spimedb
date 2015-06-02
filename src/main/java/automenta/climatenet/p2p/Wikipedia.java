/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package automenta.climatenet.p2p;


import automenta.climatenet.Core;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author me
 */
public class Wikipedia  {

    public Wikipedia() {

        //r.get("/wikipedia/:id/html", new WikiPage(e));
        //r.get("/wikipedia/search/:query", new WikiSearch(e));

    }

    public final static String[] removedClasses = {
            "IPA",
            "search-types",
            "mw-specialpage-summary",
            "mw-search-top-table",
            "#coordinates", "ambox", "noprint", "editlink",
            "thumbcaption", "magnify", "mw-editsection", "siteNotice",
            "mw-indicators", "plainlinks"
    };

    public String filterPage(Document doc) {
        String location = doc.location();
        if (location.contains("/"))
            location = location.substring(location.lastIndexOf('/') + 1, location.length());

        //String uri = "http://dbpedia.org/resource/" + location;


//        Vertex v = core.vertex( u(uri), true);
//
//        if ( !core.cached(v, "wikipedia") )  {
//            core.cache(v, "wikipedia");

        //<link rel="canonical" href="http://en.wikipedia.org/wiki/Lysergic_acid_diethylamide" />
        Elements cs = doc.getElementsByTag("link");
        if (cs != null) {
            for (Element e : cs) {
                if (e.hasAttr("rel") && e.attr("rel").equals("canonical"))
                    location = e.attr("href");
            }
        }

        try {
            //TODO combine all of these into one filter and run through all elements once
            doc.getElementsByTag("head").remove();
            doc.getElementsByTag("script").remove();
            doc.getElementsByTag("link").remove();
            doc.getElementById("top").remove();
            doc.getElementById("siteSub").remove();
            doc.getElementById("contentSub").remove();
            doc.getElementById("jump-to-nav").remove();


            for (String r : removedClasses) {
                doc.getElementsByClass(r).remove();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        removeComments(doc);

        //references and citations consume a lot of space
        Elements refs = doc.getElementsByClass("references");
        if (refs != null)
            refs.remove();


        Map<String, Object> m = new HashMap();
        m.put("url", location);


        String metadata = Core.toJSON(m);
        doc.getElementById("content").prepend("<div id='_meta'>" + metadata + "</div>");

        String content = doc.getElementById("content").toString();

        Elements catlinks = doc.select(".mw-normal-catlinks li a");
        List<String> categories = new ArrayList();
        for (Element e : catlinks) {
            if (e.tag().getName().equals("a")) {
                String c = e.attr("href");
                c = c.substring(c.lastIndexOf('/') + 1, c.length());
                categories.add(c);
            }
        }

        //v.setProperty("wikipedia_content", content);
        //for (String s : categories) {
        //Vertex c = core.vertex("dbpedia.org/resource/" + s, true);
        //core.uniqueEdge(v, c, "is");
        //}
        //core.commit();

        //req.response().end(content);

//        else {
//            System.out.println("wikipedia cached " + uri);
//            String content = v.getProperty("wikipedia_content");
//            core.commit();
//
//            if (content != null) {
//                req.response().end(content);
//            }
//            else {
//                req.response().end("Cache fail: " + uri);
//            }


        return content;
    }


    public String handleRequest(String mode, String query) throws Exception {

        //System.out.println("wikipedia handle request: " + ex + ex.getQueryString() + " " + ex.getRequestPath());

        //String[] sections = ex.getRequestPath().split("/");
            String u = null;
            switch (mode) {
                case "page":
                    //"/wikipedia/page/:pageID/html"
                    String wikipage = query;
                    u = "http://en.wikipedia.org/wiki/" + wikipage;
                    break;
                case "search":
                    //"/wikipedia/search/:query/html"
                    String q = query;
                    u = "http://en.wikipedia.org/w/index.php?search=" + q;
                    break;
            }



            byte[] response = HttpCache.the.get(u);

            Document doc = Jsoup.parse(new java.io.ByteArrayInputStream(response), Charset.defaultCharset().name(), u);

            String result = filterPage(doc);

            //SpacetimeWebServer.send(result, ex);
            return result;


    }


//
//    public class WikiPage extends HandlerThread<HttpServerRequest> {
//
//        public WikiPage(ExecutorService t) {
//            super(t);
//        }
//
//
//        @Override
//        public void run(HttpServerRequest req) {
//            try {
//                String wikipage = req.params().get("id");
//                String u = "http://en.wikipedia.org/wiki/" + wikipage;
//
//                Document doc = Jsoup.connect(u).get();
//
//                wikipage = returnPage(doc, req);
//
//                bus.publish(Bus.INTEREST_WIKIPEDIA, wikipage);
//
//            } catch (IOException ex) {
//                req.response().end(ex.toString());
//            }
//          }
//    }
//
//    public class WikiSearch extends HandlerThread<HttpServerRequest> {
//
//        public WikiSearch(ExecutorService t) {
//            super(t);
//        }
//
//        @Override
//        public void run(HttpServerRequest req) {
//            try {
//                String q = req.params().get("query");
//                String u = "http://en.wikipedia.org/w/index.php?search=" + q;
//
//                Document doc = Jsoup.connect(u).get();
//                String wikipage = returnPage(doc, req);
//
//                bus.publish(Bus.INTEREST_WIKIPEDIA, wikipage);
//
//            } catch (IOException ex) {
//                req.response().end(ex.toString());
//            }
//          }
//    }
//

    public static void removeComments(Node node) {
        // as we are removing child nodes while iterating, we cannot use a normal foreach over children,
        // or will get a concurrent list modification error.
        int i = 0;
        while (i < node.childNodes().size()) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }

//    public static String getPage(String u) {
//        URL url;
//        HttpURLConnection conn;
//        BufferedReader rd;
//        String line;
//        String result = "";
//        try {
//           url = new URL(u);
//           conn = (HttpURLConnection) url.openConnection();
//           conn.setRequestMethod("GET");
//           rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//           while ((line = rd.readLine()) != null) {
//              result += line;
//           }
//           rd.close();
//        } catch (IOException e) {
//           e.printStackTrace();
//        } catch (Exception e) {
//           e.printStackTrace();
//        }
//        return result;
//    }

}
/*
            function returnWikiPage(url, rres, redirector) {
                    http.get(url, function(res) {

                            if (res.statusCode > 300 && res.statusCode < 400 && res.headers.location) {
                                    // The location for some (most) redirects will only contain the path,  not the hostname;
                                    // detect this and add the host to the path.
                                    var u = res.headers.location;
                                    var pu = u.indexOf('/wiki/');
                                    if (pu != -1) {
                                            redirector = u.substring(pu + 6);
                                            returnWikiPage(u, rres, redirector);
                                            return;
                                    }
                            }
                            rres.writeHead(200, {'Content-Type': 'text/html'});

                            var page = '';
                            res.on("data", function(chunk) {
                                    page += chunk;
                            });
                            res.on('end', function() {
                                    var cheerio = require('cheerio');
                                    var $ = cheerio.load(page);
                                    $('script').remove();

                                    {
                                            //get the actual wikipage from the page-_____ class added to <body>
                                            var bodyclasses = $('body').attr('class').split(' ');
                                            for (var i = 0; i < bodyclasses.length; i++) {
                                                    var bc = bodyclasses[i];
                                                    if (bc.indexOf('page-') === 0) {
                                                            redirector = bc.substring(5);
                                                    }
                                            }
                                    }

                                    if (redirector)
                                            $('#content').append('<div style="display:none" class="WIKIPAGEREDIRECTOR">' + redirector + '</div>');
                                    rres.write($('#content').html() || $.html());
                                    rres.end();
                            });
                    })
            }

            express.get('/wiki/search/:query', compression, function(req, rres) {
                    var q = req.params.query;
                    returnWikiPage('http://en.wikipedia.org/w/index.php?search=' + q, rres);
            });

            express.get('/wiki/:tag/html', compression, function(req, rres) {
                    var t = req.params.tag;
                    returnWikiPage("http://en.wikipedia.org/wiki/" + t, rres);
            });
            express.get('/wiki/:tag1/:tag2/html', compression, function(req, rres) {
                    var t = req.params.tag1 + '/' + req.params.tag2;
                    returnWikiPage("http://en.wikipedia.org/wiki/" + t, rres);
            });

*/