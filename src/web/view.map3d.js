"use strict";

//this is a hack to make Cesium's require.js work with Netention's screwed up util/ client-server code-sharing
var exports = undefined;
var modules = undefined;

/** spacegraph (via cytoscape.js) */
class Map3DView extends NView {

    constructor() {
        super("Map (3D)", "globe");


    }

    start(v, app, cb) {

        var that = this;

        var init = function () {

            //   var u = uuid();
            //   var d = newDiv(u);
            //   v.append(d);

            //http://cesiumjs.org/refdoc.html
            //    var viewer = new Cesium.Viewer(u /*'cesiumContainer'*/, {
            //        timeline: false,
            //        homeButton: false,
            //        animation: false,
            //        cesiumLogo: false
            //    });
            //    $('.cesium-widget-credits').remove();
            //    $('.cesium-viewer').css('height', '100%');
            //    $(viewer.cesiumLogo).remove();
            //$(viewer.timeline).remove();
            //$(viewer.animation).remove();

            var initialTime = Cesium.JulianDate.fromDate(new Date(Date.UTC(2014, 5, 15)));
            var startTime = Cesium.JulianDate.fromDate(new Date(Date.UTC(2012, 4, 8)));
            var endTime = Cesium.JulianDate.now();
            var clock = that.clock = new Cesium.Clock({
                startTime: startTime,
                endTime: endTime,
                currentTime: endTime,
                //    multiplier: 0   // Don't start animation by default
            });
            //var imageryLayers = null;
            var previousTime = null;
            var dailyProvider = null;
            var isoDate = function (isoDateTime) {
                return isoDateTime.split("T")[0];
            };



            //add the included imagery layers to the index
            {
                var cesiumProviders = that.imagery();
                _.each(cesiumProviders, function (i) {
                    if (!i.id) i.id = i.name.replace(/ /g, '_');

                });




                /*
                var leafletProviders = _.each(new MapTileLayers(app).tags, function(l) {
                    l.newCesiumLayer = function () {
                        var turl = l.tileURL;

                        var tms = new Cesium.TileMapServiceImageryProvider({
                            url: turl
                        });


                        tms.requestImage = function(x, y, level) {
                            var url = turl.replace("{z}", level).replace("{y}", this._yFlip ? y : (1 << level) - y - 1).replace("{x}", x);
                            return Cesium.ImageryProvider.loadImage(tms, url);
                        };
                        return tms;
                    };

                });
                */

                app.addTag(_.flatten([
                    cesiumProviders,
                    //leafletProviders
                ]));

            }



            var terrainProviders = [];

            terrainProviders.push(({
                name: 'WGS84 Ellipsoid',
                icon: ('Widgets/Images/TerrainProviders/Ellipsoid.png'),
                tooltip: 'WGS84 standard ellipsoid, also known as EPSG:4326',
                newCesiumLayer: function () {
                    return new Cesium.EllipsoidTerrainProvider();
                }
            }));

            terrainProviders.push(({
                name: 'STK World Terrain meshes',
                icon: ('Widgets/Images/TerrainProviders/STK.png'),
                tooltip: 'High-resolution, mesh-based terrain for the entire globe. Free for use on the Internet. Closed-network options are available.\nhttp://www.agi.com',
                newCesiumLayer: function () {
                    return new Cesium.CesiumTerrainProvider({
                        url: '//cesiumjs.org/stk-terrain/world',
                        requestWaterMask: true,
                        requestVertexNormals: true
                    });
                }
            }));


            /*
             var onClockUpdate = _.throttle(function() {
             var isoDateTime = clock.currentTime.toString();
             var time = isoDate(isoDateTime);
             if ( time !== previousTime ) {
             previousTime = time;
             if ( dailyProvider ) {
             viewer.scene.imageryLayers.remove(dailyLayer);
             console.log('removed');
             }
             dailyLayer = viewer.scene.imageryLayers.addImageryProvider(
             provider());
             console.log('updated');
             }
             }, 1000);
             viewer.clock.onTick.addEventListener(onClockUpdate);
             onClockUpdate();

             });
             */
            var u = uuid();
            var d = newDiv(u);
            v.append(d);



            var viewer = new Cesium.Viewer(u /*'cesiumContainer'*/, {
                timeline: false,
                animation: false,

                _imageryProvider: false,
                imageryProvider : new Cesium.OpenStreetMapImageryProvider({
                    url: '//stamen-tiles.a.ssl.fastly.net/toner/',
                    credit: 'Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA.'
                }),

                baseLayerPicker: false,

                clock: clock,

                cesiumLogo: false

                //skyAtmosphere: false,
                //terrainProvider: false,
            });

            $('.cesium-widget-credits').remove();
            $('.cesium-viewer').css('height', '100%');
            $(viewer.cesiumLogo).remove();
            $(viewer.timeline).remove();
            $(viewer.animation).remove();
            viewer.scene.sunBloom = false;
            viewer.scene.skyBox = undefined;
            viewer.scene.skyAtmosphere = undefined;

//viewer.timeline.zoomTo(startTime, endTime);

// add baseLayerPicker
            //v.append('<div id="baseLayerPickerContainer"></div>');
            /*var baseLayerPicker = new Cesium.BaseLayerPicker('baseLayerPickerContainer', {
                globe: viewer.scene,
                //imageryProviderViewModels: imageryViewModels,
                terrainProviderViewModels: terrainProviders
            });*/

            //$('div.cesium-baseLayerPicker-itemLabel:contains("Terra CR (True Color)")').parent().before('<div class="cesium-baseLayerPicker-sectionTitle" data-bind="visible: terrainProviderViewModels.length > 0">NASA Satellites</div>');

            //var baseLayers = viewModel.baseLayers;


            that.viewer = viewer;
            that.layers = viewer.imageryLayers;

            app.on(['focus','change'], that.listener = function() {

                //http://cesiumjs.org/Cesium/Build/Documentation/ImageryLayerCollection.html
                //http://cesiumjs.org/Cesium/Apps/Sandcastle/index.html?src=Imagery%20Layers.html&label=undefined


                //remove only those which are no longer in app.focus
                var toRemove = [];
                for (var i = 0; i < that.layers.length; i++) {
                    var l = that.layers.get(i);
                    if (!l.tag || !app.focus[l.tag])
                        toRemove.push(l);
                }
                _.each(toRemove, function(l) {
                    that.layers.remove(l);
                });

                for (var c in app.focus) {
                    var x = app.data(c);

                    //if x is a layer..

                    if (x.newCesiumLayer) {

                        var l = x.cesiumLayer; //try to use existing layer
                        if (!l) {
                            //console.log('new layer', c);
                            x.cesiumLayerProvider = x.cesiumLayerProvider || x.newCesiumLayer();
                            l = that.layers.addImageryProvider(x.cesiumLayerProvider);
                            x.cesiumLayer = l;
                        }
                        else {
                            //console.log('existing layer', c);
                        }
                        l.tag = c;
                        l.alpha = app.focus[c]; //alpha = focus level
                    }


                }
            });

        };

        //ensure Cesium loaded
        if (!this.cesiumLoaded) {

            this.cesiumLoaded = true;

            loadCSS('lib/cesium/Widgets/widgets.css');

            $LAB
                .script('lib/cesium/Cesium.js')
                .wait(init);

        }
        else {
            init();
        }
    }

    stop(app) {
        if (this.viewer) {
            app.off(['focus', 'change'], this.listener);

            this.viewer.destroy()
            this.viewer = null;
        }
    }

    imagery() {

        var clock = this.clock;

        var ip = 'lib/cesium/';
        return [

        ({
            name: 'Stamen Toner',
            icon: ip + ('Widgets/Images/ImageryProviders/stamenToner.png'),
            tooltip: 'A high contrast black and white map.\nhttp://maps.stamen.com',
            newCesiumLayer: function () {
                return new Cesium.OpenStreetMapImageryProvider({
                    url: '//stamen-tiles.a.ssl.fastly.net/toner/',
                    credit: 'Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA.'
                });
            }
        }),

        ({
            name: 'Stamen Watercolor',
            icon: ip + ('Widgets/Images/ImageryProviders/stamenWatercolor.png'),
            tooltip: 'Reminiscent of hand drawn maps, Stamen watercolor maps apply raster effect \
    area washes and organic edges over a paper texture to add warm pop to any map.\nhttp://maps.stamen.com',
            newCesiumLayer: function () {
                return new Cesium.OpenStreetMapImageryProvider({
                    url: '//stamen-tiles.a.ssl.fastly.net/watercolor/',
                    credit: 'Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA.'
                });
            }
        }),

        ({
            name: 'Open\u00adStreet\u00adMap',
            icon: ip + ('Widgets/Images/ImageryProviders/openStreetMap.png'),
            tooltip: 'OpenStreetMap (OSM) is a collaborative project to create a free editable map \
    of the world.\nhttp://www.openstreetmap.org',
            newCesiumLayer: function () {
                return new Cesium.OpenStreetMapImageryProvider({
                    url: '//a.tile.openstreetmap.org/'
                });
            }
        }),

        ({
            name: 'MapQuest Open\u00adStreet\u00adMap',
            icon: ip + ('Widgets/Images/ImageryProviders/mapQuestOpenStreetMap.png'),
            tooltip: 'OpenStreetMap (OSM) is a collaborative project to create a free editable \
    map of the world.\nhttp://www.openstreetmap.org',
            newCesiumLayer: function () {
                return new Cesium.OpenStreetMapImageryProvider({
                    url: '//otile1-s.mqcdn.com/tiles/1.0.0/osm/'
                });
            }
        }),

        ({
            name: 'Bing Maps Aerial',
            icon: ip + ('Widgets/Images/ImageryProviders/bingAerial.png'),
            tooltip: 'Bing Maps aerial imagery \nhttp://www.bing.com/maps',
            newCesiumLayer: function () {
                return new Cesium.BingMapsImageryProvider({
                    url: '//dev.virtualearth.net',
                    mapStyle: Cesium.BingMapsStyle.AERIAL
                });
            }
        }),

        ({
            name: 'Bing Maps Aerial with Labels',
            icon: ip + ('Widgets/Images/ImageryProviders/bingAerialLabels.png'),
            tooltip: 'Bing Maps aerial imagery with label overlays \nhttp://www.bing.com/maps',
            newCesiumLayer: function () {
                return new Cesium.BingMapsImageryProvider({
                    url: '//dev.virtualearth.net',
                    mapStyle: Cesium.BingMapsStyle.AERIAL_WITH_LABELS
                });
            }
        }),

        ({
            name: 'Bing Maps Roads',
            icon: ip + ('Widgets/Images/ImageryProviders/bingRoads.png'),
            tooltip: 'Bing Maps standard road maps\nhttp://www.bing.com/maps',
            newCesiumLayer: function () {
                return new Cesium.BingMapsImageryProvider({
                    url: '//dev.virtualearth.net',
                    mapStyle: Cesium.BingMapsStyle.ROAD
                });
            }
        }),

        ({
            name: 'Natural Earth\u00a0II',
            icon: ip + ('Widgets/Images/ImageryProviders/naturalEarthII.png'),
            tooltip: 'Natural Earth II, darkened for contrast.\nhttp://www.naturalearthdata.com/',
            newCesiumLayer: function () {
                return new Cesium.TileMapServiceImageryProvider({
                    url: ip + ('Assets/Textures/NaturalEarthII')
                });
            }
        }),

        ({
            name: 'ESRI World Imagery',
            icon: ip + ('Widgets/Images/ImageryProviders/esriWorldImagery.png'),
            tooltip: '\
    World Imagery provides one meter or better satellite and aerial imagery in many parts of the world and lower resolution \
    satellite imagery worldwide.  The map includes NASA Blue Marble: Next Generation 500m resolution imagery at small scales \
    (above 1:1,000,000), i-cubed 15m eSAT imagery at medium-to-large scales (down to 1:70,000) for the world, and USGS 15m Landsat \
    imagery for Antarctica. The map features 0.3m resolution imagery in the continental United States and 0.6m resolution imagery in \
    parts of Western Europe from DigitalGlobe. In other parts of the world, 1 meter resolution imagery is available from GeoEye IKONOS, \
    i-cubed Nationwide Prime, Getmapping, AeroGRID, IGN Spain, and IGP Portugal.  Additionally, imagery at different resolutions has been \
    contributed by the GIS User Community.\nhttp://www.esri.com',
            newCesiumLayer: function () {
                return new Cesium.ArcGisMapServerImageryProvider({
                    url: '//services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
                });
            }
        }),

        ({
            name: 'ESRI World Street Map',
            icon: ip + ('Widgets/Images/ImageryProviders/esriWorldStreetMap.png'),
            tooltip: '\
    This worldwide street map presents highway-level data for the world. Street-level data includes the United States; much of \
    Canada; Japan; most countries in Europe; Australia and New Zealand; India; parts of South America including Argentina, Brazil, \
    Chile, Colombia, and Venezuela; Ghana; and parts of southern Africa including Botswana, Lesotho, Namibia, South Africa, and Swaziland.\n\
    http://www.esri.com',
            newCesiumLayer: function () {
                return new Cesium.ArcGisMapServerImageryProvider({
                    url: '//services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer'
                });
            }
        }),

        ({
            name: 'ESRI National Geographic',
            icon: ip + ('Widgets/Images/ImageryProviders/esriNationalGeographic.png'),
            tooltip: '\
    This web map contains the National Geographic World Map service. This map service is designed to be used as a general reference map \
    for informational and educational purposes as well as a basemap by GIS professionals and other users for creating web maps and web \
    mapping applications.\nhttp://www.esri.com',
            newCesiumLayer: function () {
                return new Cesium.ArcGisMapServerImageryProvider({
                    url: '//services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/'
                });
            }
        }),

        ({
            name: 'The Black Marble',
            icon: ip + ('Widgets/Images/ImageryProviders/blackMarble.png'),
            tooltip: 'The lights of cities and villages trace the outlines of civilization in this global view of the \
    Earth at night as seen by NASA/NOAA\'s Suomi NPP satellite.',
            newCesiumLayer: function () {
                return new Cesium.TileMapServiceImageryProvider({
                    url: '//cesiumjs.org/blackmarble',
                    maximumLevel: 8,
                    credit: 'Black Marble imagery courtesy NASA Earth Observatory'
                });
            }
        }),

// NASA SECTION

        ({ // start push
            name: 'Terra CR (True Color)',
            icon: 'img/terra-true-color.png',
            tooltip: 'MODIS Terra Corrected Reflectance True Color\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Terra_CorrectedReflectance_TrueColor",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Terra CR (Bands 721)',
            icon: 'img/terra-721.png',
            tooltip: 'MODIS Terra Corrected Reflectance Bands 7-2-1\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Terra_CorrectedReflectance_Bands721",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Terra CR (Bands 367)',
            icon: 'img/terra-367.png',
            tooltip: 'MODIS Terra Corrected Reflectance Bands 3-6-7\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Terra_CorrectedReflectance_Bands367",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Terra SR (Bands 121)',
            icon: 'img/terra-121.png',
            tooltip: 'MODIS Terra Land Surface Reflectance Bands 1-2-1\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Terra_SurfaceReflectance_Bands121",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Aqua CR (True Color)',
            icon: 'img/terra-true-color.png',
            tooltip: 'MODIS Aqua Corrected Reflectance True Color\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Aqua_CorrectedReflectance_TrueColor",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Aqua CR (Bands 721)',
            icon: 'img/terra-721.png',
            tooltip: 'MODIS Aqua Corrected Reflectance Bands 7-2-1\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Aqua_CorrectedReflectance_Bands721",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Aqua SR (Bands 721)',
            icon: 'img/terra-721.png',
            tooltip: 'MODIS Aqua Land Surface Reflectance Bands 7-2-1\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Aqua_SurfaceReflectance_Bands721",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        }),

        ({ // start push
            name: 'Aqua SR (Bands 121)',
            icon: 'img/terra-121.png',
            tooltip: 'MODIS Aqua Land Surface Reflectance Bands 1-2-1\n Adjust time slider to desired date before selecting this layer to view satellite data on that date. \n credit: NASA Earth Observing System Data and Information System (EOSDIS) Global Imagery Browse Services (GIBS)',
            newCesiumLayer: function () {
                var isoDateTime = clock.currentTime.toString();
                var time = "TIME=" + isoDate(isoDateTime);
                return new Cesium.WebMapTileServiceImageryProvider({
                    url: "//map1.vis.earthdata.nasa.gov/wmts-webmerc/wmts.cgi?" + time,
                    layer: "MODIS_Terra_SurfaceReflectance_Bands121",
                    style: "",
                    format: "image/jpeg",
                    tileMatrixSetID: "GoogleMapsCompatible_Level9",
                    maximumLevel: 9,
                    tileWidth: 256,
                    tileHeight: 256,
                    tilingScheme: new Cesium.WebMercatorTilingScheme()
                });
            }
        })

    ]; }
}

function isoDate(isoDateTime) {return isoDateTime.split("T")[0];};