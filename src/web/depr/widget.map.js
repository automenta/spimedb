var ASTRONOMICAL_DISTANCE = 99999999.0; //in km

function setGeolocatedLocation(map, onUpdated) {
    var geolocate = new OpenLayers.Control.Geolocate({
        bind: false,
        geolocationOptions: {
            enableHighAccuracy: false,
            maximumAge: 0,
            timeout: 7000
        }
    });

    geolocate.events.register('locationupdated', geolocate, onUpdated);

    geolocate.events.register('locationfailed', this, function() {
        OpenLayers.Console.log('Location detection failed');
    });

    map.addControl(geolocate);

    geolocate.activate();

}


function initLocationChooserMap(target, location, zoom, geolocate) {
    
    var map = L.map(target, {
        attributionControl: false
    }).setView(location, 12);

    map.addControl(newLeafletGeoCoder());

    if (geolocate) {
        map.on('locationfound', function(e) {
            var p = e.latlng;
            clickAt(p);
            map.stopLocate();
        });
        map.locate({
            setView: true,
            enableHighAccuracy: true
        });
    }

    var marker = L.marker(location);
    marker.addTo(map);

    //var location = {lat: location[0], lng: location[1]};

    function clickAt(p) {
        location = {lat: p.lat, lng: p.lng};
        marker.setLatLng(location);
        if (map.onClick) {
            map.onClick(location);
        }
    }


    map.location = function() {
        return {lat: location.lat, lon: location.lng};
    };

    map.on('click', function(e) {
        map.stopLocate();
        var p = e.latlng;
        clickAt(p);
    });


    /*L.tileLayer('http://{s}.tile.cloudmade.com/{key}/22677/256/{z}/{x}/{y}.png', {
     attribution: 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2012 CloudMade',
     key: 'BC9A493B41014CAABB98F0471D759707'
     }).addTo(map);*/

    //http://leaflet-extras.github.io/leaflet-providers/preview/index.htmlfile
    var baseLayer = L.tileLayer('http://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Tiles courtesy of <a href="http://hot.openstreetmap.org/" target="_blank">Humanitarian OpenStreetMap Team</a>'
    });
    baseLayer.addTo(map);

    //TODO call this to cleanup garbage
    map.onDestroy = function() {
        map = null;
    };

    return map;
}

function newLeafletGeoCoder() {
    return new L.Control.OSMGeocoder({
        collapsed: true, /* Whether its collapsed or not */
        position: 'topright', /* The position of the control */
        text: 'Go', /* The text of the submit button */
        bounds: null, /* a L.LatLngBounds object to limit the results to */
        email: null, /* an email string with a contact to provide to Nominatim. Useful if you are doing lots of queries */
        callback: function(results) {
            var bbox = results[0].boundingbox,
                    first = new L.LatLng(bbox[0], bbox[2]),
                    second = new L.LatLng(bbox[1], bbox[3]),
                    bounds = new L.LatLngBounds([first, second]);
            this._map.fitBounds(bounds);
        }
    });
}


var gp1, gp2;

//distance, in kilometers
function geoDist(p1, p2) {
    if (!gp1) {
        gp1 = L.latLng(0, 0);
        gp2 = L.latLng(0, 0);
    }

    if (p1[0] == p2[0])
        if (p1[1] == p2[1])
            return 0;

    gp1.lat = p1[0];
    gp1.lng = p1[1];
    gp2.lat = p2[0];
    gp2.lng = p2[1];

    //http://dev.openlayers.org/docs/files/OpenLayers/Util-js.html#Util.distVincenty
    return gp1.distanceTo(gp2) / 1000.0;
}

//var gp1 = { }, gp2 = { };
function geoDistOL(p1, p2) {
    if (p1[0] == p2[0])
        if (p1[1] == p2[1])
            return 0;

    gp1.lat = p1[0];
    gp1.lon = p1[1];
    gp2.lat = p2[0];
    gp2.lon = p2[1];

    //http://dev.openlayers.org/docs/files/OpenLayers/Util-js.html#Util.distVincenty
    if (OpenLayers)
        return OpenLayers.Util.distVincenty(gp1, gp2);
    else
        return 0;
}
