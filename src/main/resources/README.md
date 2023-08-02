# 🐈🎮 | GLOBAL SURVIVAL SYSTEM | CLIMATE EDITOR 3D | SPACETIME TAG PLANNER

Real-time decentralized global propagation, notification, and analysis tools for AUTONOMY MAXIMIZATION and AUTHORITY DISSOLUTION.

Learn how natural and artificial phenomena, from past to present, underground to outer space... might affect the quality and length of our futures.

# Layers
 * yields Objects and Tags (adaptively cached in digraph).  
 * refreshed periodically or when focus location changes.

### Home
For control of the focus: position (incl. range), time (incl. range)

Typically this will be user's actual location.

### WorldWind Geo
https://worldwind.earth/worldwindjs/
 * streets
 * satellite
 * etc. 

### OpenStreetMaps (OSM)
loads vector map features from the Overpass API https://dev.overpass-api.de/overpass-doc/en/

### WordNet
WordNet provides a more complete scaffolding vocabulary through an embedded WordNet instance: synonyms, antonyms, etc.
ultimately this is meant to support user expressivity and the ergonomics of the user interface.
https://wordnet.princeton.edu/

### Value Heatmap
Finite-size 2D/3D heatmap (scalar mesh).
This aggregates the effects of user Values at various spatial positions, so its local minima and maxima can bias routing and other activity.

The shape and inner detail of the mesh is determined by the use-case: from neighborhood to international.

It can be displayed on the map as a color-coded overlay on the terrain.

### Route
similar to the Home layer, this represents the location and paths toward (explicit, _or implicit_) destinations.
it provides access to all route planning and route visualization parameters.
this can interact with a real-time navigation system, which might utilize text-to-speech etc.

### Photogrammetry
3D model construction from multiple-POV photos or video

### ClimateViewer3D Geo Layers
most of the file types it uses with Cesium.js are supported by WorldWind.js

### Comms
buddy list and group chat functionality through various endpoints, including a native, optionally anonymous, WebRTC gossip mesh with (optional) hyperlocal affinity.
include live streaming of audio and video and broadcast alerts (ex: https://getcell411.com)

## Objects
 * various kinds of media
 * includes user created tweets/notes that can be tagged with location.  
 * by default: private, but public notes are useful in cooperative planning.  (ex: WebRTC)
 * these notes can utilize an extensive semantic ontology and include multimedia
 * they can be used to add factual environment data, or make corrections to source datasets (ex: OSM) 

## Tag
Hierarchical category containing Objects and other Tags.  URI-like
 * enabled: default=true, but if false then mostly hidden
 * pri: numeric priority, which can be assigned by graph metrics or otherwise
 
Ideally, tags are chosen to resolve to (English) Wikipedia pages allowing them to be contextualized in a wikipedia-based ontology.  Since wikipages are already categorized and inter-linked, as well as multilingual.

## Values
A user's current preferences expressed as a declarative set value of assignments.

```{ (x,f,y), ... }```
   * x: tag or other identifier
   * f: ie, function, dimension, predicate, verb...
     * goal: seek(+)/avoid(-) (GSS)
     * know: learn(+)/teach(-) (curiosume)
     * have: can(+)/need(-) (curiosume++)
     * TODO other semantic dimensions  
   * y: number from -1 to +1 (0 is neutral and has no effect)
 
these can be serialized for sharing or as re-usable presets or templates

    
## Rendering Loop
Ideally a real-time, high-frequency interactive experience.  
So that adjusting any part of the model instantly affects the other parts. 
```
if (focus position or scope or time changed) {
    refresh layers
}

if (tags changed) {
    re-render tag widgets
        adaptive hierarchical menu (DOM elements)
}

if (values changed) {
    for each Object, apply value assignments and predictions to adjust:
        visibility/opacity
        color
        labels, popup menus, etc.       
}
```       
    
# Links
 * https://climateviewer.com
 * https://github.com/Global-Survival/GSs/
 * https://sharewiki.org/en/Spime_Housing
 * https://getcell411.com
