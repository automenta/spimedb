/******/ (function(modules) { // webpackBootstrap
/******/ 	// install a JSONP callback for chunk loading
/******/ 	var parentJsonpFunction = window["webpackJsonp"];
/******/ 	window["webpackJsonp"] = function webpackJsonpCallback(chunkIds, moreModules, executeModules) {
/******/ 		// add "moreModules" to the modules object,
/******/ 		// then flag all "chunkIds" as loaded and fire callback
/******/ 		var moduleId, chunkId, i = 0, resolves = [], result;
/******/ 		for(;i < chunkIds.length; i++) {
/******/ 			chunkId = chunkIds[i];
/******/ 			if(installedChunks[chunkId]) {
/******/ 				resolves.push(installedChunks[chunkId][0]);
/******/ 			}
/******/ 			installedChunks[chunkId] = 0;
/******/ 		}
/******/ 		for(moduleId in moreModules) {
/******/ 			if(Object.prototype.hasOwnProperty.call(moreModules, moduleId)) {
/******/ 				modules[moduleId] = moreModules[moduleId];
/******/ 			}
/******/ 		}
/******/ 		if(parentJsonpFunction) parentJsonpFunction(chunkIds, moreModules, executeModules);
/******/ 		while(resolves.length) {
/******/ 			resolves.shift()();
/******/ 		}
/******/
/******/ 	};
/******/
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// objects to store loaded and loading chunks
/******/ 	var installedChunks = {
/******/ 		4: 0
/******/ 	};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/ 	// This file contains only the entry chunk.
/******/ 	// The chunk loading function for additional chunks
/******/ 	__webpack_require__.e = function requireEnsure(chunkId) {
/******/ 		var installedChunkData = installedChunks[chunkId];
/******/ 		if(installedChunkData === 0) {
/******/ 			return new Promise(function(resolve) { resolve(); });
/******/ 		}
/******/
/******/ 		// a Promise means "currently loading".
/******/ 		if(installedChunkData) {
/******/ 			return installedChunkData[2];
/******/ 		}
/******/
/******/ 		// setup Promise in chunk cache
/******/ 		var promise = new Promise(function(resolve, reject) {
/******/ 			installedChunkData = installedChunks[chunkId] = [resolve, reject];
/******/ 		});
/******/ 		installedChunkData[2] = promise;
/******/
/******/ 		// start chunk loading
/******/ 		var head = document.getElementsByTagName('head')[0];
/******/ 		var script = document.createElement('script');
/******/ 		script.type = 'text/javascript';
/******/ 		script.charset = 'utf-8';
/******/ 		script.async = true;
/******/ 		script.timeout = 120000;
/******/
/******/ 		if (__webpack_require__.nc) {
/******/ 			script.setAttribute("nonce", __webpack_require__.nc);
/******/ 		}
/******/ 		script.src = __webpack_require__.p + "spime." + chunkId + ".js";
/******/ 		var timeout = setTimeout(onScriptComplete, 120000);
/******/ 		script.onerror = script.onload = onScriptComplete;
/******/ 		function onScriptComplete() {
/******/ 			// avoid mem leaks in IE.
/******/ 			script.onerror = script.onload = null;
/******/ 			clearTimeout(timeout);
/******/ 			var chunk = installedChunks[chunkId];
/******/ 			if(chunk !== 0) {
/******/ 				if(chunk) {
/******/ 					chunk[1](new Error('Loading chunk ' + chunkId + ' failed.'));
/******/ 				}
/******/ 				installedChunks[chunkId] = undefined;
/******/ 			}
/******/ 		};
/******/ 		head.appendChild(script);
/******/
/******/ 		return promise;
/******/ 	};
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// identity function for calling harmony imports with the correct context
/******/ 	__webpack_require__.i = function(value) { return value; };
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// on error function for async loading
/******/ 	__webpack_require__.oe = function(err) { console.error(err); throw err; };
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 8);
/******/ })
/************************************************************************/
/******/ ({

/***/ 8:
/***/ (function(module, exports, __webpack_require__) {

__webpack_require__.e/* import() */(0).then(__webpack_require__.bind(null, 0)).then(Spime => {

    function itemURL(id, path) {
        return encodeURIComponent(id) + '/' + path;
    }

    $.get('/logo.html', (x) => {
        $('#logo').html(x);
    });

    const query = $('#query');
    const facets = $('#facets');
    const queryText = $('#query_text');
    const suggestions = $('#query_suggestions');

    const onQueryTextChanged = _.throttle(() => {
        const qText = queryText.val();
        if (!qText.length) {
            suggestions.html('');
            return;
        }

        //$('#query_status').html('Suggesting: ' + qText);

        $.get('/suggest', {q: qText}, function (result) {

            if (!result.length) {
                suggestions.html('');
            } else {
                suggestions.html(_.map((result), (x) =>
                    D('suggestion').text(x).click(() => {
                        queryText.val(x);
                        update(x);
                    })
                ));
            }
        });
    }, 100, true, true);

    const querySubmit = () => {
        update(queryText.val());
    };

    queryText.on('input', onQueryTextChanged);

    queryText.on('keypress', (e) => {
        if (e.keyCode === 13)
            querySubmit();
    });

    function scrollTop() {
        $("body").scrollTop(0);
    }

    function expand() {
        query.addClass('expand');
//            $('#menu').removeClass('sidebar');
//            $('#menu').addClass('expand');
//            $('#results').hide();
//
//            $('#facets .list-item').removeClass('list-item').addClass('grid-item');

    }

    function contract() {
        query.removeClass('expand');
//            unfocus();
//            $('#results').show().addClass('main');
//            $('#menu').removeClass('expand');
//            $('#menu').addClass('sidebar');
//
//            $('#facets .grid-item').removeClass('grid-item').attr('style', '').addClass('list-item');
    }

    function focus(id) {
        //Backbone.history.navigate("the/" + encodeURIComponent(id), { trigger: true });

        window.open(itemURL(id, 'data'), '_blank', 'menubar=no,status=no,titlebar=no');
    }


    function suggestionsClear() {
        suggestions.html('');
    }

    function update(qText) {

        Backbone.history.navigate("all/" + encodeURIComponent(qText), {trigger: true});

    }

    function facet(v) {

        const id = v[0]
            .replace(/_/g, ' ')
            .replace(/\-/g, ' ')
        ; //HACK
        const score = v[1];

        return E('button', 'facet')
            .text(id)
            .attr('style',
                'font-size:' + (50.0 + 20 * (Math.log(1.0 + score)) ) + '%'
            )
            .click(() => {
                queryText.val(/* dimension + ':' + */ id);
                querySubmit();
                return false;
            });
    };

    function loadFacets(result) {
        facets.html(_.map(result, facet));


        //setTimeout(()=>{

//            setTimeout(()=>{ facets.packery('layout');
//
//                setTimeout(()=>{ facets.packery('layout'); }, 300);
//
//            }, 300);
        //}, 0);
    }

//PACKERY.js
//http://codepen.io/desandro/pen/vKjAPE/
//http://packery.metafizzy.co/extras.html#animating-item-size

    function updateFacet(dimension) {

        //const klass = label;

//            $('#facets.' + klass).remove();
//
//            const f = $('<svg width="250" height="250">').attr('class', klass);//.html(label + '...');
//            $('#facets').append($('<div>').append(f));

        FACETS({q: dimension}, loadFacets);

    }


//START ----------------->
    const $results = $('#results');

    const Router = Backbone.Router.extend({

        routes: {
            "": "start",
            "all/:query": "all",
            "the/:query": "the"
        },

//            the: function(id) {
//
//                suggestionsClear();
//
//
//
////                $('#focus').attr('class', 'main').html(
////                    E('iframe').attr('src', dataURL).attr('width', '100%').attr('height', '100%')
////                );
////                $('#results').attr('class', 'sidebar shiftdown');
////                $('#menu').attr('class', 'hide');
////                $('#focus').show();
//
//            },

        all: function (qText) {

            suggestionsClear();

            contract();

            scrollTop();

            //$('#query_status').html('').append($('<p>').text('Query: ' + qText));
            $results.html('Searching...');

            $.get('/find', {q: qText}, function (result) {

                let ss, rr, ff;
                try {
                    ss = (result);
                    rr = ss[0]; //first part: search results
                    ff = ss[1]; //second part: facets
                } catch (e) {
                    //usually just empty search result
                    $results.html('No matches for: "' + qText + '"');
                    return;
                }

                contract();

                loadFacets(ff);

                const clusters = {};

                $results.html(
                    _.map(rr, (x) => {

                        if (!x.I)
                            return;

                        const y = D('list-item result');
                        y.data('o', x);

                        let I = x.I;
                        if (x.inh) {
                            x.out = x.inh['>'];

                            const vin = x.inh['<'];
                            if (vin && !(vin.length === 1 && vin[0].length === 0)) { //exclude root tag
                                x['in'] = vin;
                                I = vin;
                            }
                        }


                        if (clusters[I] === undefined) {
                            clusters[I] = [y];
                        } else {
                            clusters[I].push(y);
                        }


                        const header = D('header');

                        header.append(
                            E('h2').text(x.N || x.I)
                        );


                        const meta = D('meta');


                        y.append(
                            header,
                            meta
                        );

                        if (x.icon) {
                            const tt =
                                    //E('a').attr('class', 'fancybox').attr('rel', 'group').append(
                                    E('img').attr('src', itemURL(I, 'icon'))
                                //)
                            ;
                            y.append(
                                tt
                            );

                            //http://fancyapps.com/fancybox/#examples
                            //tt.fancybox();
                        }

                        if (x['_']) {
                            y.append(E('p').attr('class', 'textpreview').html(x['_'].replace('\n', '<br/>')));
                        }


                        if (x.data) {
                            y.click(() => {
                                focus(x.data);
                            });
                        }


                        return y;
                    })
                );

                _.each(clusters, (c, k) => {

                    if (c.length < 2)
                        return; //ignore clusters of length < 2

                    const start = c[0];

                    const d = D('list-item result');
                    $(start).before(d);
                    c.forEach(cc => {
                        /* {

                         d = cc;
                         } else {
                         children.push(cc);
                         }*/
                        cc.detach();
                        cc.addClass('sub');
                        if (cc.data('o').I !== k) //the created root entry for this cluster, ignore for now
                            d.append(cc);
                    });

                    //HACK if there was only 1 child, just pop it back to top-level subsuming any parents
                    const dc = d.children();
                    if (dc.length === 1) {
                        $(dc[0]).removeClass('sub');
                        d.replaceWith(dc[0]);
                    }


                });


            });

        },

        start: function () {

            facets.html('');

            expand();

            //updateFacet('I', 'Category');
            updateFacet('>', 'Tag');


        }


    });

    const router = new Router();

    Backbone.history.start();

});

/***/ })

/******/ });