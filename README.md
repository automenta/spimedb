P2P Geographic Situational Awareness System
===========================================

Real-time decentralized global propagation, notification, and analysis tools for the most comprehensive, cross-domain, uncensored environmental dataset ever assembled - ClimateViewer (used in ClimateViewer 3D and ClimateViewer Mobile).

Discover how natural and artificial phenomena, from past to present, underground to outer space... might affect the quality and length of our futures.

http://climateviewer.com/about/

**FEATURES**
------------

**Server**
 *   Finished*   ..
 *   Somewhat Ready
	* KML Import
	    *   basics including Point geometry&nbsp;
    *   minify description HTML content
      *   HTML tokenization prevents indexing of HTML tags
        * 
    *   P2P Protocol*   Publishing a query and getting a response
        *   Transfer layers and geodata (pull: receiver initiates)
    *   Web Server*   Static files
        *   Geo queries*   Circle (top left to bottom right of client's web map's = diameter of search, centered at center)
        *   Layer (metadata) queries
 *   TODO*   Application runtime options*   Enable web server*   bind: host, port
        *   Enable embedded ES*   ES configuration
        *   Enable local/remote ES's*   list of ES addresses (host:port)
        *   Enable peer node*   bind: host, port
            *   list of seed peer addresses (host:port)
    *   KML Import*   remaining geometry types*   Line, Multiline, Polygon, ...
            *   Overlay
        *   verify that network links are correctly traversed
        *   styles and their resources available on static web server
        *
    *   Web server*   Bound box query: more accurate for map display than circle (especially when the less square a box is)
    *   Installer script + documentation
    *   Seed Peerlist on Github
    *   Layer metadata*   Semantic URI's - Apache GeoTK ISO's and NASA SWEET
        *   tags
    *   SensorML*   ..
    *   More KML/GeoRSS/etc.. layers*
    *   NLP and Numeric Analysis of Measurements
    *   Document indexing (via Apache Tika plugin to ES)

**Client**
 *   Somewhat Ready
    *   Leaflet map
    *   dynamic HTTP GET AJAX update, triggering redraw of the layers
    *   Simple top-level layer legend of visibile item types with item count
 *   TODO*   Websockets as preferred client/server communication protocol*   Streaming client pull for view and interest (focus) updates
        *   Streaming server push for alerts and notifications
    *   Layer tree*   variable opacity
        *   activation of request for non-visualized (but possibly available) data types
        *   All metadata shown or clickable popup that shows, ex: URL and attribution source
    *   Icons and styling
    *   Item creation / editing (available in leaflet plugin)
    *   Representation of server equipment by its geolocation (geoip)
    *   Additional tilesets (available in leaflet plugin)
    *   Leaflet*   Remaining GeoJSON types
        *   Overlay rendering
        *   Normalize Lat/Lon (it goes out of bounds if continue around earth)
    *   Cesium*   ..


## Run in App mode
```
chromium --app=http://localhost:9090/graph.html --incognito
```
