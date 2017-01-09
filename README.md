# SpimeDB - realtime spatiotemporal database

![SpimeDB Logo](https://raw.githubusercontent.com/automenta/spimedb/master/data/spimedb_logo.png)

SpimeDB unifies the semantic, spatial, and temporal dimensions of data in a common n-dimensional ranged vector space.

Exploiting hyperdimensional locality enables efficient iterative access in display, analysis, and distributed computation applications.

## NObject (json) specification v-0.00
```js

	// ID, but if it's missing it can be assigned randomly (ex: UUID).
	I: <URL/UUID>,

	// Default name, label, title.  If missing, the ID is used
	N: "name",

	// Tags (array of strings, to the id's of tags from which this intensionally inherits)
	'<': [ tag1, tag2, ... ],

	// Description: string or a internationalized string table, ex:
	'_': { _: "xx" /* optional 2 letter country code indicating the default language involved in this nobject, which may be blank to represent complete internationalization */,
		  en: "english label",
		  es: "etiqueta en español",
		  ...
	},

	// Author, origin, provenance: one or more nobject IDs. URL or email string works fine
	A: 'http://url/id',

	//Editing times.
	//  A number, or an array of numbers in milliseconds.
	//  First # is creation time, subsequent # indicate the delta offset to
	//    the last edit time (to save digits).
	//  Millisecond (ms) or lesser precision: hexadecimal long integer
	//  Finer than millisecond (ex: nanosecond): floating point number
	E: [ 238472389472, 23423432 ],

	//Expiration time in unixtime milliseconds.  After this time point, the nobject permits its deletion.  If zero, the object is considered temporary and can be deleted at any point.
	X: 23842738423,

	//Privacy / visiblity directives and public keys
	P: [ 'private', 'anonymous', ... ],

    // Spacetime bounds 4 (four) dimensional vector of spatial (0, 1, 2) and temporal (3) ranged values.
	'@': [
	    1.3,                            //point, matches within a finite epsilon range determined by floating poitn precision
	    [ -558.0, 2494.0 ],             //bounded range
	    [ -Infinity, +Infinifty ],      //matches any
	    NaN                             //matches any (shorter)
	]

```

## Spacetime vector space semantics

projection models (choose a default or assign these id's for encoding type):

### lat/lon/alt/unixtime
 * requires modulo +/-180 degrees longitude which may require preprocessing certain queries
 * distortion at poles

### x/y/z/unixtime - WSG geographic projection to (flat) euclidean 3D absolute space
 * TODO which is the newest projection model

### other models
 * ?

assuming flat temporal dynamics model: unixtime, or millisecond epochs since ~1970 will exhibit variations in precision if stored
as a 32-bit float, but 64-bit float ('double')' is better but not as ideal as 64-bit integer
('long').


## Additional payload data can be set at arbitary keys in the NObject:
```json

	// arbitrary named data values as fields with associated value
	key1: value,
	property2: data
```

##Graph (draft, essentially macros for node-centric graph declarative extensions)
```js
	//contents, contained, children, hyperedge internal adjacencies
	C: { /* one or more nobjects */ }

	//containers, parents, hyperedge external adjacencies
	P: { /* one or more nobjects */ }

	//incoming source directed edge ("link")
	//   the strength value can be used to indicate
	//    < 1.0 (default) strength / weight / priority / importance
	S: {
		//predicate  identifies an nobject
		predicate1: {
			[ //single or array object

				"identified_source_nobject",

				[ 0.85f, "another_identified_source_with_given_strength" ]

				{ /* inline object, with an effective ID prefixed by the outer object */ },

			]
		},
		predicate2: {
			[ "targetID", "targetID" ]
		}
	},

	//destination / outgoing target directed edge ("link")
	D: { /*..*/ },

	//undirected graph edge
	U: { /*..*/ },
```


## $N: NQuery NObject functions (draft)
```js
$N({ }) - wraps an instance of class N wrapping the given nobject data. throws exceptions if the data is invalid

$N(string) - resolves by id, unless the string begins with '{' in which case it is parsed as JSON

$N() - returns new NObject.HashNObject with random ID

$N([obj1, obj2, ..]) - returns a new container object with the supplied items as the children

n.uuid() //UUID, formed from the id plus some combination of time and authorship

n.json() //JSON representation of the nobject

n.equal(m) //equality test

n.merge(m[, merge parameters]) //returns an object containing the merge or union of two nobjects. any conflicts result in a conflict field with an array of describing each conflict such that no data is destroyed. if nothing can be merged, the result is equivalent to $N([n, m])

n.xml() //XML representation, which can be directly inserted into an HTML page DOM as a web widget

n.graph(target[, enabled graphed features and parameters]) //adds to the specified graph object nodes and edges representing 'n' and its connections. if target is falsy, it creates a new graph

n.clone(newID) //clone with new ID. if falsy: randomly generated ID and adds blank (anonymous) author '_' to the end of the author list, which will possibly be replaced with the actual author at some point prior to storage or transmission otherwise it will remain anonymous

n.modified() //sets the modification time to current system time in a new cloned object. optional parameter to specify this (backdate or forward date)

n.narsese() //narsese representation (string)

n.rdf(options) //RDF representation, options indicating which serialization (default: JSON RDF)

n.edges(incoming,outgoing,undirected, edgeVisitor) //iterate certain kinds of edges:
    //  edgeVisitor(rootNobject,incoming,outgoing,undirected,
    //					otherNobject,predicate[,strength])
    //		if returns falsy, iteration is cancelled

n.attr() //returns a new object containing only the non-nobject attributes
n.attr(x) //returns the data value associated with attribute key (if string), or sets the values if parameter is a JS object
n.attr(key, value) //sets the data value

n.distanceSpace(m) //returns space distance in meters. NaN if either has no spatial information, positive value if there is separation, 0 if fully coincident, and negative if partially overlapping as a percentage scaled in proportion (or inverse?) to the mean radii

n.distanceTime(m) //similar to space distance but for 1D time regions in seconds.

//tags are those edges which are stored in an array in .D.a (or just .D if the value is an array and not an object)
    //	this means the outgoing 'a' edges
    //  ('a' is the indefinite article predicate, as in owl:isA,  "is a", or "isA")

n.tagSet() - returns an array containing the tag strings

n.tagStrengths() - returns an object containing a mapping of the tags to the strength values, ex: { tag1: 1, tag2: 0.5 }

n.tag(tagID) - returns the strength (default 1.0) of the tag if present, or undefined otherwise, 0.0 or falsy being equivalent to undefined/null

n.tag(tagID[, strength]) - sets the tag strength of a particular tag

```
----

Real-time decentralized global propagation, notification, and analysis tools for AUTONOMY MAXIMIZATION and AUTHORITY DISSOLUTION.

Discover how natural and artificial phenomena, from past to present, underground to outer space... might affect the quality and length of our futures.

## See also
http://sharewiki.org/en/Spime_Housing
