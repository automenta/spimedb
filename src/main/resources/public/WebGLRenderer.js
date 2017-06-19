/* global PIXI,Renderer */

module.exports = function (p2) {

    // shim layer with setTimeout fallback
    var requestAnimFrame =  window.requestAnimationFrame       ||
        window.webkitRequestAnimationFrame ||
        window.mozRequestAnimationFrame    ||
        window.oRequestAnimationFrame      ||
        window.msRequestAnimationFrame     ||
        function( callback ){
            window.setTimeout(callback, 1000 / 60);
        };

    var disableSelectionCSS = [
        "-ms-user-select: none",
        "-moz-user-select: -moz-none",
        "-khtml-user-select: none",
        "-webkit-user-select: none",
        "user-select: none"
    ];

    p2.Renderer = Renderer;


    /**
     * Base class for rendering a p2 physics scene.
     * @class Renderer
     * @constructor
     * @param {object} scenes One or more scene definitions. See setScene.
     */
    function Renderer(scenes, options){
        p2.EventEmitter.call(this);

        options = options || {};

        // Expose globally
        window.app = this;

        var that = this;

        if(scenes.setup){
            // Only one scene given, without name
            scenes = {
                'default': scenes
            };
        } else if(typeof(scenes)==='function'){
            scenes = {
                'default': {
                    setup: scenes
                }
            };
        }

        this.scenes = scenes;

        this.state = Renderer.DEFAULT;

        // Bodies to draw
        this.bodies=[];
        this.springs=[];
        this.timeStep = 1/60;
        this.relaxation = p2.Equation.DEFAULT_RELAXATION;
        this.stiffness = p2.Equation.DEFAULT_STIFFNESS;

        this.mouseConstraint = null;
        this.nullBody = new p2.Body();
        this.pickPrecision = 5;

        this.useInterpolatedPositions = true;

        this.drawPoints = [];
        this.drawPointsChangeEvent = { type : "drawPointsChange" };
        this.drawCircleCenter = p2.vec2.create();
        this.drawCirclePoint = p2.vec2.create();
        this.drawCircleChangeEvent = { type : "drawCircleChange" };
        this.drawRectangleChangeEvent = { type : "drawRectangleChange" };
        this.drawRectStart = p2.vec2.create();
        this.drawRectEnd = p2.vec2.create();

        this.stateChangeEvent = { type : "stateChange", state:null };


        // Default collision masks for new shapes
        this.newShapeCollisionMask = 1;
        this.newShapeCollisionGroup = 1;

        // If constraints should be drawn
        this.drawConstraints = false;

        this.stats_sum = 0;
        this.stats_N = 100;
        this.stats_Nsummed = 0;
        this.stats_average = -1;

        this.addedGlobals = [];

        this.settings = {
            tool: Renderer.DEFAULT,
            fullscreen: function(){
                var el = document.body;
                var requestFullscreen = el.requestFullscreen || el.msRequestFullscreen || el.mozRequestFullScreen || el.webkitRequestFullscreen;
                if(requestFullscreen){
                    requestFullscreen.call(el);
                }
            },

            'paused [p]': false,
            'manualStep [s]': function(){ that.world.step(that.world.lastTimeStep); },
            fps: 60,
            maxSubSteps: 3,
            gravityX: 0,
            gravityY: -10,
            sleepMode: p2.World.NO_SLEEPING,

            'drawContacts [c]': false,
            'drawAABBs [t]': false,
            drawConstraints: false,

            iterations: 10,
            stiffness: 1000000,
            relaxation: 4,
            tolerance: 0.0001,
        };

        this.init();
        this.resizeToFit();
        this.render();
        this.createStats();
        this.addLogo();
        this.centerCamera(0, 0);

        window.onresize = function(){
            that.resizeToFit();
        };

        this.setUpKeyboard();
        this.setupGUI();

        if(typeof(options.hideGUI) === 'undefined'){
            options.hideGUI = 'auto';
        }
        if((options.hideGUI === 'auto' && window.innerWidth < 600) || options.hideGUI === true){
            this.gui.close();
        }

        this.printConsoleMessage();

        // Set first scene
        this.setSceneByIndex(0);

        this.startRenderingLoop();

    }
    Renderer.prototype = new p2.EventEmitter();

    Renderer.DEFAULT =            1;
    Renderer.PANNING =            2;
    Renderer.DRAGGING =           3;
    Renderer.DRAWPOLYGON =        4;
    Renderer.DRAWINGPOLYGON  =    5;
    Renderer.DRAWCIRCLE =         6;
    Renderer.DRAWINGCIRCLE  =     7;
    Renderer.DRAWRECTANGLE =      8;
    Renderer.DRAWINGRECTANGLE  =  9;

    Renderer.toolStateMap = {
        'pick/pan [q]': Renderer.DEFAULT,
        'polygon [d]': Renderer.DRAWPOLYGON,
        'circle [a]': Renderer.DRAWCIRCLE,
        'rectangle [f]': Renderer.DRAWRECTANGLE
    };
    Renderer.stateToolMap = {};
    for(var key in Renderer.toolStateMap){
        Renderer.stateToolMap[Renderer.toolStateMap[key]] = key;
    }

    Renderer.keydownEvent = {
        type:"keydown",
        originalEvent : null,
        keyCode : 0,
    };
    Renderer.keyupEvent = {
        type:"keyup",
        originalEvent : null,
        keyCode : 0,
    };

    Object.defineProperty(Renderer.prototype, 'drawContacts', {
        get: function() {
            return this.settings['drawContacts [c]'];
        },
        set: function(value) {
            this.settings['drawContacts [c]'] = value;
            this.updateGUI();
        }
    });

    Object.defineProperty(Renderer.prototype, 'drawAABBs', {
        get: function() {
            return this.settings['drawAABBs [t]'];
        },
        set: function(value) {
            this.settings['drawAABBs [t]'] = value;
            this.updateGUI();
        }
    });

    Object.defineProperty(Renderer.prototype, 'paused', {
        get: function() {
            return this.settings['paused [p]'];
        },
        set: function(value) {
            this.resetCallTime = true;
            this.settings['paused [p]'] = value;
            this.updateGUI();
        }
    });

    Renderer.prototype.getDevicePixelRatio = function() {
        return window.devicePixelRatio || 1;
    };

    Renderer.prototype.printConsoleMessage = function(){
        console.log([
            '=== p2.js v' + p2.version + ' ===',
            'Welcome to the p2.js debugging environment!',
            'Did you know you can interact with the physics here in the console? Try executing the following:',
            '',
            '  world.gravity[1] = 10;',
            ''
        ].join('\n'));
    };

    Renderer.prototype.resizeToFit = function(){
        var dpr = this.getDevicePixelRatio();
        var rect = this.elementContainer.getBoundingClientRect();
        var w = rect.width * dpr;
        var h = rect.height * dpr;
        this.resize(w, h);
    }

    /**
     * Sets up dat.gui
     */
    Renderer.prototype.setupGUI = function() {
        if(typeof(dat) === 'undefined'){
            return;
        }

        var that = this;

        var gui = this.gui = new dat.GUI();
        gui.domElement.setAttribute('style',disableSelectionCSS.join(';'));

        var settings = this.settings;

        gui.add(settings, 'tool', Renderer.toolStateMap).onChange(function(state){
            that.setState(parseInt(state));
        });
        gui.add(settings, 'fullscreen');

        // World folder
        var worldFolder = gui.addFolder('World');
        worldFolder.open();
        worldFolder.add(settings, 'paused [p]').onChange(function(p){
            that.paused = p;
        });
        worldFolder.add(settings, 'manualStep [s]');
        worldFolder.add(settings, 'fps', 10, 60*10).step(10).onChange(function(freq){
            that.timeStep = 1 / freq;
        });
        worldFolder.add(settings, 'maxSubSteps', 0, 10).step(1);
        var maxg = 100;

        function changeGravity(){
            if(!isNaN(settings.gravityX) && !isNaN(settings.gravityY)){
                p2.vec2.set(that.world.gravity, settings.gravityX, settings.gravityY);
            }
        }
        worldFolder.add(settings, 'gravityX', -maxg, maxg).onChange(changeGravity);
        worldFolder.add(settings, 'gravityY', -maxg, maxg).onChange(changeGravity);
        worldFolder.add(settings, 'sleepMode', {
            NO_SLEEPING: p2.World.NO_SLEEPING,
            BODY_SLEEPING: p2.World.BODY_SLEEPING,
            ISLAND_SLEEPING: p2.World.ISLAND_SLEEPING,
        }).onChange(function(mode){
            that.world.sleepMode = parseInt(mode);
        });

        // Rendering
        var renderingFolder = gui.addFolder('Rendering');
        renderingFolder.open();
        renderingFolder.add(settings,'drawContacts [c]').onChange(function(draw){
            that.drawContacts = draw;
        });
        renderingFolder.add(settings,'drawAABBs [t]').onChange(function(draw){
            that.drawAABBs = draw;
        });

        // Solver
        var solverFolder = gui.addFolder('Solver');
        solverFolder.open();
        solverFolder.add(settings, 'iterations', 1, 100).step(1).onChange(function(it){
            that.world.solver.iterations = it;
        });
        solverFolder.add(settings, 'stiffness', 10).onChange(function(k){
            that.setEquationParameters();
        });
        solverFolder.add(settings, 'relaxation', 0, 20).step(0.1).onChange(function(d){
            that.setEquationParameters();
        });
        solverFolder.add(settings, 'tolerance', 0, 10).step(0.01).onChange(function(t){
            that.world.solver.tolerance = t;
        });

        // Scene picker
        var sceneFolder = gui.addFolder('Scenes');
        sceneFolder.open();

        // Add scenes
        var i = 1;
        for(var sceneName in this.scenes){
            var guiLabel = sceneName + ' [' + (i++) + ']';
            this.settings[guiLabel] = function(){
                that.setScene(that.scenes[sceneName]);
            };
            sceneFolder.add(settings, guiLabel);
        }
    };

    /**
     * Updates dat.gui. Call whenever you change demo.settings.
     */
    Renderer.prototype.updateGUI = function() {
        if(!this.gui){
            return;
        }
        function updateControllers(folder){
            // First level
            for (var i in folder.__controllers){
                folder.__controllers[i].updateDisplay();
            }

            // Second level
            for (var f in folder.__folders){
                updateControllers(folder.__folders[f]);
            }
        }
        updateControllers(this.gui);
    };

    Renderer.prototype.setWorld = function(world){
        this.world = world;

        window.world = world; // For debugging.

        var that = this;

        world.on("postStep",function(e){
            that.updateStats();
        }).on("addBody",function(e){
            that.addVisual(e.body);
        }).on("removeBody",function(e){
            that.removeVisual(e.body);
        }).on("addSpring",function(e){
            that.addVisual(e.spring);
        }).on("removeSpring",function(e){
            that.removeVisual(e.spring);
        });
    };

    /**
     * Sets the current scene to the scene definition given.
     * @param {object} sceneDefinition
     * @param {function} sceneDefinition.setup
     * @param {function} [sceneDefinition.teardown]
     */
    Renderer.prototype.setScene = function(sceneDefinition){
        if(typeof(sceneDefinition) === 'string'){
            sceneDefinition = this.scenes[sceneDefinition];
        }

        this.removeAllVisuals();
        if(this.currentScene && this.currentScene.teardown){
            this.currentScene.teardown();
        }
        if(this.world){
            this.world.clear();
        }

        for(var i=0; i<this.addedGlobals.length; i++){
            delete window[this.addedGlobals];
        }

        var preGlobalVars = Object.keys(window);

        this.currentScene = sceneDefinition;
        this.world = null;
        sceneDefinition.setup.call(this);
        if(!this.world){
            throw new Error('The .setup function in the scene definition must run this.setWorld(world);');
        }

        var postGlobalVars = Object.keys(window);
        var added = [];
        for(var i = 0; i < postGlobalVars.length; i++){
            if(preGlobalVars.indexOf(postGlobalVars[i]) === -1 && postGlobalVars[i] !== 'world'){
                added.push(postGlobalVars[i]);
            }
        }
        if(added.length){
            added.sort();
            console.log([
                'The following variables were exposed globally from this physics scene.',
                '',
                '  ' + added.join(', '),
                ''
            ].join('\n'));
        }

        this.addedGlobals = added;

        // Set the GUI parameters from the loaded world
        var settings = this.settings;
        settings.iterations = this.world.solver.iterations;
        settings.tolerance = this.world.solver.tolerance;
        settings.gravityX = this.world.gravity[0];
        settings.gravityY = this.world.gravity[1];
        settings.sleepMode = this.world.sleepMode;
        this.updateGUI();
    };

    /**
     * Set scene by its position in which it was given. Starts at 0.
     * @param {number} index
     */
    Renderer.prototype.setSceneByIndex = function(index){
        var i = 0;
        for(var key in this.scenes){
            if(i === index){
                this.setScene(this.scenes[key]);
                break;
            }
            i++;
        }
    };

    Renderer.elementClass = 'p2-canvas';
    Renderer.containerClass = 'p2-container';

    /**
     * Adds all needed keyboard callbacks
     */
    Renderer.prototype.setUpKeyboard = function() {
        var that = this;

        this.elementContainer.onkeydown = function(e){
            if(!e.keyCode){
                return;
            }
            var s = that.state;
            var ch = String.fromCharCode(e.keyCode);
            switch(ch){
                case "P": // pause
                    that.paused = !that.paused;
                    break;
                case "S": // step
                    that.world.step(that.world.lastTimeStep);
                    break;
                case "R": // restart
                    that.setScene(that.currentScene);
                    break;
                case "C": // toggle draw contacts & constraints
                    that.drawContacts = !that.drawContacts;
                    that.drawConstraints = !that.drawConstraints;
                    break;
                case "T": // toggle draw AABBs
                    that.drawAABBs = !that.drawAABBs;
                    break;
                case "D": // toggle draw polygon mode
                    that.setState(s === Renderer.DRAWPOLYGON ? Renderer.DEFAULT : s = Renderer.DRAWPOLYGON);
                    break;
                case "A": // toggle draw circle mode
                    that.setState(s === Renderer.DRAWCIRCLE ? Renderer.DEFAULT : s = Renderer.DRAWCIRCLE);
                    break;
                case "F": // toggle draw rectangle mode
                    that.setState(s === Renderer.DRAWRECTANGLE ? Renderer.DEFAULT : s = Renderer.DRAWRECTANGLE);
                    break;
                case "Q": // set default
                    that.setState(Renderer.DEFAULT);
                    break;
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                case "6":
                case "7":
                case "8":
                case "9":
                    that.setSceneByIndex(parseInt(ch) - 1);
                    break;
                default:
                    Renderer.keydownEvent.keyCode = e.keyCode;
                    Renderer.keydownEvent.originalEvent = e;
                    that.emit(Renderer.keydownEvent);
                    break;
            }
            that.updateGUI();
        };

        this.elementContainer.onkeyup = function(e){
            if(e.keyCode){
                switch(String.fromCharCode(e.keyCode)){
                    default:
                        Renderer.keyupEvent.keyCode = e.keyCode;
                        Renderer.keyupEvent.originalEvent = e;
                        that.emit(Renderer.keyupEvent);
                        break;
                }
            }
        };
    };

    /**
     * Start the rendering loop
     */
    Renderer.prototype.startRenderingLoop = function(){
        var demo = this,
            lastCallTime = Date.now() / 1000;

        function update(){
            if(!demo.paused){
                var now = Date.now() / 1000,
                    timeSinceLastCall = now - lastCallTime;
                if(demo.resetCallTime){
                    timeSinceLastCall = 0;
                    demo.resetCallTime = false;
                }
                lastCallTime = now;
                demo.world.step(demo.timeStep, timeSinceLastCall, demo.settings.maxSubSteps);
            }
            demo.render();
            requestAnimFrame(update);
        }
        requestAnimFrame(update);
    };

    /**
     * Set the app state.
     * @param {number} state
     */
    Renderer.prototype.setState = function(state){
        this.state = state;
        this.stateChangeEvent.state = state;
        this.emit(this.stateChangeEvent);
        if(Renderer.stateToolMap[state]){
            this.settings.tool = state;
            this.updateGUI();
        }
    };

    /**
     * Should be called by subclasses whenever there's a mousedown event
     */
    Renderer.prototype.handleMouseDown = function(physicsPosition){
        switch(this.state){

            case Renderer.DEFAULT:

                // Check if the clicked point overlaps bodies
                var result = this.world.hitTest(physicsPosition, this.world.bodies, this.pickPrecision);

                // Remove static bodies
                var b;
                while(result.length > 0){
                    b = result.shift();
                    if(b.type === p2.Body.STATIC){
                        b = null;
                    } else {
                        break;
                    }
                }

                if(b){
                    b.wakeUp();
                    this.setState(Renderer.DRAGGING);
                    // Add mouse joint to the body
                    var localPoint = p2.vec2.create();
                    b.toLocalFrame(localPoint,physicsPosition);
                    this.world.addBody(this.nullBody);
                    this.mouseConstraint = new p2.RevoluteConstraint(this.nullBody, b, {
                        localPivotA: physicsPosition,
                        localPivotB: localPoint
                    });
                    this.world.addConstraint(this.mouseConstraint);
                } else {
                    this.setState(Renderer.PANNING);
                }
                break;

            case Renderer.DRAWPOLYGON:
                // Start drawing a polygon
                this.setState(Renderer.DRAWINGPOLYGON);
                this.drawPoints = [];
                var copy = p2.vec2.create();
                p2.vec2.copy(copy,physicsPosition);
                this.drawPoints.push(copy);
                this.emit(this.drawPointsChangeEvent);
                break;

            case Renderer.DRAWCIRCLE:
                // Start drawing a circle
                this.setState(Renderer.DRAWINGCIRCLE);
                p2.vec2.copy(this.drawCircleCenter,physicsPosition);
                p2.vec2.copy(this.drawCirclePoint, physicsPosition);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWRECTANGLE:
                // Start drawing a circle
                this.setState(Renderer.DRAWINGRECTANGLE);
                p2.vec2.copy(this.drawRectStart,physicsPosition);
                p2.vec2.copy(this.drawRectEnd, physicsPosition);
                this.emit(this.drawRectangleChangeEvent);
                break;
        }
    };

    /**
     * Should be called by subclasses whenever there's a mousedown event
     */
    Renderer.prototype.handleMouseMove = function(physicsPosition){
        var sampling = 0.4;
        switch(this.state){
            case Renderer.DEFAULT:
            case Renderer.DRAGGING:
                if(this.mouseConstraint){
                    p2.vec2.copy(this.mouseConstraint.pivotA, physicsPosition);
                    this.mouseConstraint.bodyA.wakeUp();
                    this.mouseConstraint.bodyB.wakeUp();
                }
                break;

            case Renderer.DRAWINGPOLYGON:
                // drawing a polygon - add new point
                var sqdist = p2.vec2.dist(physicsPosition,this.drawPoints[this.drawPoints.length-1]);
                if(sqdist > sampling*sampling){
                    var copy = [0,0];
                    p2.vec2.copy(copy,physicsPosition);
                    this.drawPoints.push(copy);
                    this.emit(this.drawPointsChangeEvent);
                }
                break;

            case Renderer.DRAWINGCIRCLE:
                // drawing a circle - change the circle radius point to current
                p2.vec2.copy(this.drawCirclePoint, physicsPosition);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWINGRECTANGLE:
                // drawing a rectangle - change the end point to current
                p2.vec2.copy(this.drawRectEnd, physicsPosition);
                this.emit(this.drawRectangleChangeEvent);
                break;
        }
    };

    /**
     * Should be called by subclasses whenever there's a mouseup event
     */
    Renderer.prototype.handleMouseUp = function(physicsPosition){

        var b;

        switch(this.state){

            case Renderer.DEFAULT:
                break;

            case Renderer.DRAGGING:
                // Drop constraint
                this.world.removeConstraint(this.mouseConstraint);
                this.mouseConstraint = null;
                this.world.removeBody(this.nullBody);
                this.setState(Renderer.DEFAULT);
                break;

            case Renderer.PANNING:
                this.setState(Renderer.DEFAULT);
                break;

            case Renderer.DRAWINGPOLYGON:
                // End this drawing state
                this.setState(Renderer.DRAWPOLYGON);
                if(this.drawPoints.length > 3){
                    // Create polygon
                    b = new p2.Body({ mass : 1 });
                    if(b.fromPolygon(this.drawPoints,{
                            removeCollinearPoints : 0.01,
                        })){
                        this.world.addBody(b);
                    }
                }
                this.drawPoints = [];
                this.emit(this.drawPointsChangeEvent);
                break;

            case Renderer.DRAWINGCIRCLE:
                // End this drawing state
                this.setState(Renderer.DRAWCIRCLE);
                var R = p2.vec2.dist(this.drawCircleCenter,this.drawCirclePoint);
                if(R > 0){
                    // Create circle
                    b = new p2.Body({ mass : 1, position : this.drawCircleCenter });
                    var circle = new p2.Circle({ radius: R });
                    b.addShape(circle);
                    this.world.addBody(b);
                }
                p2.vec2.copy(this.drawCircleCenter,this.drawCirclePoint);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWINGRECTANGLE:
                // End this drawing state
                this.setState(Renderer.DRAWRECTANGLE);
                // Make sure first point is upper left
                var start = this.drawRectStart;
                var end = this.drawRectEnd;
                for(var i=0; i<2; i++){
                    if(start[i] > end[i]){
                        var tmp = end[i];
                        end[i] = start[i];
                        start[i] = tmp;
                    }
                }
                var width = Math.abs(start[0] - end[0]);
                var height = Math.abs(start[1] - end[1]);
                if(width > 0 && height > 0){
                    // Create box
                    b = new p2.Body({
                        mass : 1,
                        position : [this.drawRectStart[0] + width*0.5, this.drawRectStart[1] + height*0.5]
                    });
                    var rectangleShape = new p2.Box({ width: width, height:  height });
                    b.addShape(rectangleShape);
                    this.world.addBody(b);
                }
                p2.vec2.copy(this.drawRectEnd,this.drawRectStart);
                this.emit(this.drawRectangleChangeEvent);
                break;
        }

        if(b){
            b.wakeUp();
            for(var i=0; i<b.shapes.length; i++){
                var s = b.shapes[i];
                s.collisionMask =  this.newShapeCollisionMask;
                s.collisionGroup = this.newShapeCollisionGroup;
            }
        }
    };

    /**
     * Update stats
     */
    Renderer.prototype.updateStats = function(){
        this.stats_sum += this.world.lastStepTime;
        this.stats_Nsummed++;
        if(this.stats_Nsummed === this.stats_N){
            this.stats_average = this.stats_sum/this.stats_N;
            this.stats_sum = 0.0;
            this.stats_Nsummed = 0;
        }
        /*
        this.stats_stepdiv.innerHTML = "Physics step: "+(Math.round(this.stats_average*100)/100)+"ms";
        this.stats_contactsdiv.innerHTML = "Contacts: "+this.world.narrowphase.contactEquations.length;
        */
    };

    /**
     * Add an object to the demo
     * @param  {mixed} obj Either Body or Spring
     */
    Renderer.prototype.addVisual = function(obj){
        if(obj instanceof p2.LinearSpring){
            this.springs.push(obj);
            this.addRenderable(obj);
        } else if(obj instanceof p2.Body){
            if(obj.shapes.length){ // Only draw things that can be seen
                this.bodies.push(obj);
                this.addRenderable(obj);
            }
        }
    };

    /**
     * Removes all visuals from the scene
     */
    Renderer.prototype.removeAllVisuals = function(){
        var bodies = this.bodies,
            springs = this.springs;
        while(bodies.length){
            this.removeVisual(bodies[bodies.length-1]);
        }
        while(springs.length){
            this.removeVisual(springs[springs.length-1]);
        }
    };

    /**
     * Remove an object from the demo
     * @param  {mixed} obj Either Body or Spring
     */
    Renderer.prototype.removeVisual = function(obj){
        this.removeRenderable(obj);
        if(obj instanceof p2.LinearSpring){
            var idx = this.springs.indexOf(obj);
            if(idx !== -1){
                this.springs.splice(idx,1);
            }
        } else if(obj instanceof p2.Body){
            var idx = this.bodies.indexOf(obj);
            if(idx !== -1){
                this.bodies.splice(idx,1);
            }
        } else {
            console.error("Visual type not recognized...");
        }
    };

    /**
     * Create the container/divs for stats
     * @todo  integrate in new menu
     */
    Renderer.prototype.createStats = function(){
        /*
        var stepDiv = document.createElement("div");
        var vecsDiv = document.createElement("div");
        var matsDiv = document.createElement("div");
        var contactsDiv = document.createElement("div");
        stepDiv.setAttribute("id","step");
        vecsDiv.setAttribute("id","vecs");
        matsDiv.setAttribute("id","mats");
        contactsDiv.setAttribute("id","contacts");
        document.body.appendChild(stepDiv);
        document.body.appendChild(vecsDiv);
        document.body.appendChild(matsDiv);
        document.body.appendChild(contactsDiv);
        this.stats_stepdiv = stepDiv;
        this.stats_contactsdiv = contactsDiv;
        */
    };

    Renderer.prototype.addLogo = function(){
        var css = [
            'position:absolute',
            'left:10px',
            'top:15px',
            'text-align:center',
            'font: 13px Helvetica, arial, freesans, clean, sans-serif',
        ].concat(disableSelectionCSS);

        var div = document.createElement('div');
        div.innerHTML = [
            "<div style='"+css.join(';')+"' user-select='none'>",
            "<h1 style='margin:0px'><a href='http://github.com/schteppe/p2.js' style='color:black; text-decoration:none;'>p2.js</a></h1>",
            "<p style='margin:5px'>Physics Engine</p>",
            '<a style="color:black; text-decoration:none;" href="https://twitter.com/share" class="twitter-share-button" data-via="schteppe" data-count="none" data-hashtags="p2js">Tweet</a>',
            "</div>"
        ].join("");
        this.elementContainer.appendChild(div);

        // Twitter button script
        !function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');
    };

    Renderer.zoomInEvent = {
        type:"zoomin"
    };
    Renderer.zoomOutEvent = {
        type:"zoomout"
    };

    Renderer.prototype.setEquationParameters = function(){
        this.world.setGlobalStiffness(this.settings.stiffness);
        this.world.setGlobalRelaxation(this.settings.relaxation);
    };


    p2.WebGLRenderer = WebGLRenderer;

    var Renderer = p2.Renderer;

    /**
     * Renderer using Pixi.js
     * @class WebGLRenderer
     * @constructor
     * @extends Renderer
     * @param {World}   scenes
     * @param {Object}  [options]
     * @param {Number}  [options.lineWidth=0.01]
     * @param {Number}  [options.scrollFactor=0.1]
     * @param {Number}  [options.width]               Num pixels in horizontal direction
     * @param {Number}  [options.height]              Num pixels in vertical direction
     */
    function WebGLRenderer(scenes, options) {
        options = options || {};

        var that = this;

        var settings = {
            lineWidth: 0.01,
            scrollFactor: 0.1,
            width: 1280, // Pixi screen resolution
            height: 720,
            useDeviceAspect: false,
            sleepOpacity: 0.2,
        };
        for (var key in options) {
            settings[key] = options[key];
        }

        if (settings.useDeviceAspect) {
            settings.height = window.innerHeight / window.innerWidth * settings.width;
        }

        //this.settings = settings;
        this.lineWidth = settings.lineWidth;
        this.scrollFactor = settings.scrollFactor;
        this.sleepOpacity = settings.sleepOpacity;

        this.sprites = [];
        this.springSprites = [];
        this.debugPolygons = false;

        Renderer.call(this, scenes, options);

        for (var key in settings) {
            this.settings[key] = settings[key];
        }

        this.pickPrecision = 0.1;

        // Update "ghost draw line"
        this.on("drawPointsChange", function (e) {
            var g = that.drawShapeGraphics;
            var path = that.drawPoints;

            g.clear();

            var path2 = [];
            for (var j = 0; j < path.length; j++) {
                var v = path[j];
                path2.push([v[0], v[1]]);
            }

            that.drawPath(g, path2, 0xff0000, false, that.lineWidth, false);
        });

        // Update draw circle
        this.on("drawCircleChange", function (e) {
            var g = that.drawShapeGraphics;
            g.clear();
            var center = that.drawCircleCenter;
            var R = p2.vec2.dist(center, that.drawCirclePoint);
            that.drawCircle(g, center[0], center[1], 0, R, false, that.lineWidth);
        });

        // Update draw circle
        this.on("drawRectangleChange", function (e) {
            var g = that.drawShapeGraphics;
            g.clear();
            var start = that.drawRectStart;
            var end = that.drawRectEnd;
            var width = start[0] - end[0];
            var height = start[1] - end[1];
            that.drawRectangle(g, start[0] - width / 2, start[1] - height / 2, 0, width, height, false, false, that.lineWidth, false);
        });
    }

    WebGLRenderer.prototype = Object.create(Renderer.prototype);

    WebGLRenderer.prototype.stagePositionToPhysics = function (out, stagePosition) {
        var x = stagePosition[0],
            y = stagePosition[1];
        p2.vec2.set(out, x, y);
        return out;
    };

    /**
     * Initialize the renderer and stage
     */
    var init_stagePosition = p2.vec2.create(),
        init_physicsPosition = p2.vec2.create();
    WebGLRenderer.prototype.init = function () {
        var w = this.w,
            h = this.h,
            s = this.settings;

        var that = this;

        var renderer = this.renderer = PIXI.autoDetectRenderer(s.width, s.height, null, null, true);
        var stage = this.stage = new PIXI.Container();
        var container = this.container = new PIXI.Stage(0xFFFFFF, true);

        var el = this.element = this.renderer.view;
        el.tabIndex = 1;
        el.classList.add(Renderer.elementClass);
        el.setAttribute('style', 'width:100%;');

        var div = this.elementContainer = document.createElement('div');
        div.classList.add(Renderer.containerClass);
        div.setAttribute('style', 'width:100%; height:100%');
        div.appendChild(el);
        document.body.appendChild(div);
        el.focus();
        el.oncontextmenu = function (e) {
            return false;
        };

        this.container.addChild(stage);

        // Graphics object for drawing shapes
        this.drawShapeGraphics = new PIXI.Graphics();
        stage.addChild(this.drawShapeGraphics);

        // Graphics object for contacts
        this.contactGraphics = new PIXI.Graphics();
        stage.addChild(this.contactGraphics);

        // Graphics object for AABBs
        this.aabbGraphics = new PIXI.Graphics();
        stage.addChild(this.aabbGraphics);

        stage.scale.x = 200; // Flip Y direction.
        stage.scale.y = -200;

        var lastX, lastY, lastMoveX, lastMoveY, startX, startY, down = false;

        var physicsPosA = p2.vec2.create();
        var physicsPosB = p2.vec2.create();
        var stagePos = p2.vec2.create();
        var initPinchLength = 0;
        var initScaleX = 1;
        var initScaleY = 1;
        var lastNumTouches = 0;
        container.mousedown = container.touchstart = function (e) {
            lastMoveX = e.global.x;
            lastMoveY = e.global.y;

            if (e.originalEvent.touches) {
                lastNumTouches = e.originalEvent.touches.length;
            }

            if (e.originalEvent.touches && e.originalEvent.touches.length === 2) {

                var touchA = that.container.interactionManager.touchs[0];
                var touchB = that.container.interactionManager.touchs[1];

                var pos = touchA.getLocalPosition(stage);
                p2.vec2.set(stagePos, pos.x, pos.y);
                that.stagePositionToPhysics(physicsPosA, stagePos);

                var pos = touchB.getLocalPosition(stage);
                p2.vec2.set(stagePos, pos.x, pos.y);
                that.stagePositionToPhysics(physicsPosB, stagePos);

                initPinchLength = p2.vec2.dist(physicsPosA, physicsPosB);

                var initScaleX = stage.scale.x;
                var initScaleY = stage.scale.y;

                return;
            }
            lastX = e.global.x;
            lastY = e.global.y;
            startX = stage.position.x;
            startY = stage.position.y;
            down = true;

            that.lastMousePos = e.global;

            var pos = e.getLocalPosition(stage);
            p2.vec2.set(init_stagePosition, pos.x, pos.y);
            that.stagePositionToPhysics(init_physicsPosition, init_stagePosition);
            that.handleMouseDown(init_physicsPosition);
        };
        container.mousemove = container.touchmove = function (e) {
            if (e.originalEvent.touches) {
                if (lastNumTouches !== e.originalEvent.touches.length) {
                    lastX = e.global.x;
                    lastY = e.global.y;
                    startX = stage.position.x;
                    startY = stage.position.y;
                }

                lastNumTouches = e.originalEvent.touches.length;
            }

            lastMoveX = e.global.x;
            lastMoveY = e.global.y;

            if (e.originalEvent.touches && e.originalEvent.touches.length === 2) {
                var touchA = that.container.interactionManager.touchs[0];
                var touchB = that.container.interactionManager.touchs[1];

                var pos = touchA.getLocalPosition(stage);
                p2.vec2.set(stagePos, pos.x, pos.y);
                that.stagePositionToPhysics(physicsPosA, stagePos);

                var pos = touchB.getLocalPosition(stage);
                p2.vec2.set(stagePos, pos.x, pos.y);
                that.stagePositionToPhysics(physicsPosB, stagePos);

                var pinchLength = p2.vec2.dist(physicsPosA, physicsPosB);

                // Get center
                p2.vec2.add(physicsPosA, physicsPosA, physicsPosB);
                p2.vec2.scale(physicsPosA, physicsPosA, 0.5);
                that.zoom(
                    (touchA.global.x + touchB.global.x) * 0.5,
                    (touchA.global.y + touchB.global.y) * 0.5,
                    null,
                    pinchLength / initPinchLength * initScaleX, // zoom relative to the initial scale
                    pinchLength / initPinchLength * initScaleY
                );

                return;
            }

            if (down && that.state === Renderer.PANNING) {
                stage.position.x = e.global.x - lastX + startX;
                stage.position.y = e.global.y - lastY + startY;
            }

            that.lastMousePos = e.global;

            var pos = e.getLocalPosition(stage);
            p2.vec2.set(init_stagePosition, pos.x, pos.y);
            that.stagePositionToPhysics(init_physicsPosition, init_stagePosition);
            that.handleMouseMove(init_physicsPosition);
        };
        container.mouseup = container.touchend = function (e) {
            if (e.originalEvent.touches) {
                lastNumTouches = e.originalEvent.touches.length;
            }

            down = false;
            lastMoveX = e.global.x;
            lastMoveY = e.global.y;

            that.lastMousePos = e.global;

            var pos = e.getLocalPosition(stage);
            p2.vec2.set(init_stagePosition, pos.x, pos.y);
            that.stagePositionToPhysics(init_physicsPosition, init_stagePosition);
            that.handleMouseUp(init_physicsPosition);
        };

        // http://stackoverflow.com/questions/7691551/touchend-event-in-ios-webkit-not-firing
        this.element.ontouchmove = function (e) {
            e.preventDefault();
        };

        function MouseWheelHandler(e) {
            // cross-browser wheel delta
            e = window.event || e; // old IE support
            //var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail)));

            var o = e,
                d = o.detail, w = o.wheelDelta,
                n = 225, n1 = n - 1;

            // Normalize delta: http://stackoverflow.com/a/13650579/2285811
            var f;
            d = d ? w && (f = w / d) ? d / f : -d / 1.35 : w / 120;
            // Quadratic scale if |d| > 1
            d = d < 1 ? d < -1 ? (-Math.pow(d, 2) - n1) / n : d : (Math.pow(d, 2) + n1) / n;
            // Delta *should* not be greater than 2...
            var delta = Math.min(Math.max(d / 2, -1), 1);

            var out = delta >= 0;
            if (typeof lastMoveX !== 'undefined') {
                that.zoom(lastMoveX, lastMoveY, out, undefined, undefined, delta);
            }
        }

        if (el.addEventListener) {
            el.addEventListener("mousewheel", MouseWheelHandler, false); // IE9, Chrome, Safari, Opera
            el.addEventListener("DOMMouseScroll", MouseWheelHandler, false); // Firefox
        } else {
            el.attachEvent("onmousewheel", MouseWheelHandler); // IE 6/7/8
        }

        this.centerCamera(0, 0);
    };

    WebGLRenderer.prototype.zoom = function (x, y, zoomOut, actualScaleX, actualScaleY, multiplier) {
        var scrollFactor = this.scrollFactor,
            stage = this.stage;

        if (typeof actualScaleX === 'undefined') {

            if (!zoomOut) {
                scrollFactor *= -1;
            }

            scrollFactor *= Math.abs(multiplier);

            stage.scale.x *= (1 + scrollFactor);
            stage.scale.y *= (1 + scrollFactor);
            stage.position.x += (scrollFactor) * (stage.position.x - x);
            stage.position.y += (scrollFactor) * (stage.position.y - y);
        } else {
            stage.scale.x *= actualScaleX;
            stage.scale.y *= actualScaleY;
            stage.position.x += (actualScaleX - 1) * (stage.position.x - x);
            stage.position.y += (actualScaleY - 1) * (stage.position.y - y);
        }

        stage.updateTransform();
    };

    WebGLRenderer.prototype.centerCamera = function (x, y) {
        this.stage.position.x = this.renderer.width / 2 - this.stage.scale.x * x;
        this.stage.position.y = this.renderer.height / 2 - this.stage.scale.y * y;

        this.stage.updateTransform();
    };

    /**
     * Make sure that a rectangle is visible in the canvas.
     * @param  {number} centerX
     * @param  {number} centerY
     * @param  {number} width
     * @param  {number} height
     */
    WebGLRenderer.prototype.frame = function (centerX, centerY, width, height) {
        var ratio = this.renderer.width / this.renderer.height;
        if (ratio < width / height) {
            this.stage.scale.x = this.renderer.width / width;
            this.stage.scale.y = -this.stage.scale.x;
        } else {
            this.stage.scale.y = -this.renderer.height / height;
            this.stage.scale.x = -this.stage.scale.y;
        }
        this.centerCamera(centerX, centerY);
    };

    /**
     * Draw a circle onto a graphics object
     * @method drawCircle
     * @static
     * @param  {PIXI.Graphics} g
     * @param  {Number} x
     * @param  {Number} y
     * @param  {Number} radius
     * @param  {Number} color
     * @param  {Number} lineWidth
     */
    WebGLRenderer.prototype.drawCircle = function (g, x, y, angle, radius, color, lineWidth, isSleeping) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "number" ? color : 0xffffff;
        g.lineStyle(lineWidth, 0x000000, 1);
        g.beginFill(color, isSleeping ? this.sleepOpacity : 1.0);
        g.drawCircle(x, y, radius);
        g.endFill();

        // line from center to edge
        g.moveTo(x, y);
        g.lineTo(x + radius * Math.cos(angle),
            y + radius * Math.sin(angle));
    };

    WebGLRenderer.drawSpring = function (g, restLength, color, lineWidth) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0xffffff : color;
        g.lineStyle(lineWidth, color, 1);
        if (restLength < lineWidth * 10) {
            restLength = lineWidth * 10;
        }
        var M = 12;
        var dx = restLength / M;
        g.moveTo(-restLength / 2, 0);
        for (var i = 1; i < M; i++) {
            var x = -restLength / 2 + dx * i;
            var y = 0;
            if (i <= 1 || i >= M - 1) {
                // Do nothing
            } else if (i % 2 === 0) {
                y -= 0.1 * restLength;
            } else {
                y += 0.1 * restLength;
            }
            g.lineTo(x, y);
        }
        g.lineTo(restLength / 2, 0);
    };

    /**
     * Draw a finite plane onto a PIXI.Graphics.
     * @method drawPlane
     * @param  {PIXI.Graphics} g
     * @param  {Number} x0
     * @param  {Number} x1
     * @param  {Number} color
     * @param  {Number} lineWidth
     * @param  {Number} diagMargin
     * @param  {Number} diagSize
     * @todo Should consider an angle
     */
    WebGLRenderer.drawPlane = function (g, x0, x1, color, lineColor, lineWidth, diagMargin, diagSize, maxLength) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0xffffff : color;
        g.lineStyle(lineWidth, lineColor, 1);

        // Draw a fill color
        g.lineStyle(0, 0, 0);
        g.beginFill(color);
        var max = maxLength;
        g.moveTo(-max, 0);
        g.lineTo(max, 0);
        g.lineTo(max, -max);
        g.lineTo(-max, -max);
        g.endFill();

        // Draw the actual plane
        g.lineStyle(lineWidth, lineColor);
        g.moveTo(-max, 0);
        g.lineTo(max, 0);
    };


    WebGLRenderer.drawLine = function (g, offset, angle, len, color, lineWidth) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0x000000 : color;
        g.lineStyle(lineWidth, color, 1);

        var startPoint = p2.vec2.fromValues(-len / 2, 0);
        var endPoint = p2.vec2.fromValues(len / 2, 0);

        p2.vec2.rotate(startPoint, startPoint, angle);
        p2.vec2.rotate(endPoint, endPoint, angle);

        p2.vec2.add(startPoint, startPoint, offset);
        p2.vec2.add(endPoint, endPoint, offset);

        g.moveTo(startPoint[0], startPoint[1]);
        g.lineTo(endPoint[0], endPoint[1]);
    };

    WebGLRenderer.prototype.drawCapsule = function (g, x, y, angle, len, radius, color, fillColor, lineWidth, isSleeping) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0x000000 : color;
        g.lineStyle(lineWidth, color, 1);

        // Draw circles at ends
        var c = Math.cos(angle);
        var s = Math.sin(angle);
        g.beginFill(fillColor, isSleeping ? this.sleepOpacity : 1.0);
        g.drawCircle(-len / 2 * c + x, -len / 2 * s + y, radius);
        g.drawCircle(len / 2 * c + x, len / 2 * s + y, radius);
        g.endFill();

        // Draw rectangle
        g.lineStyle(lineWidth, color, 0);
        g.beginFill(fillColor, isSleeping ? this.sleepOpacity : 1.0);
        g.moveTo(-len / 2 * c + radius * s + x, -len / 2 * s + radius * c + y);
        g.lineTo(len / 2 * c + radius * s + x, len / 2 * s + radius * c + y);
        g.lineTo(len / 2 * c - radius * s + x, len / 2 * s - radius * c + y);
        g.lineTo(-len / 2 * c - radius * s + x, -len / 2 * s - radius * c + y);
        g.endFill();

        // Draw lines in between
        g.lineStyle(lineWidth, color, 1);
        g.moveTo(-len / 2 * c + radius * s + x, -len / 2 * s + radius * c + y);
        g.lineTo(len / 2 * c + radius * s + x, len / 2 * s + radius * c + y);
        g.moveTo(-len / 2 * c - radius * s + x, -len / 2 * s - radius * c + y);
        g.lineTo(len / 2 * c - radius * s + x, len / 2 * s - radius * c + y);

    };

// Todo angle
    WebGLRenderer.prototype.drawRectangle = function (g, x, y, angle, w, h, color, fillColor, lineWidth, isSleeping) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "number" ? color : 0xffffff;
        fillColor = typeof(fillColor) === "number" ? fillColor : 0xffffff;
        g.lineStyle(lineWidth);
        g.beginFill(fillColor, isSleeping ? this.sleepOpacity : 1.0);
        g.drawRect(x - w / 2, y - h / 2, w, h);
    };

    WebGLRenderer.prototype.drawConvex = function (g, verts, triangles, color, fillColor, lineWidth, debug, offset, isSleeping) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0x000000 : color;
        if (!debug) {
            g.lineStyle(lineWidth, color, 1);
            g.beginFill(fillColor, isSleeping ? this.sleepOpacity : 1.0);
            for (var i = 0; i !== verts.length; i++) {
                var v = verts[i],
                    x = v[0],
                    y = v[1];
                if (i === 0) {
                    g.moveTo(x, y);
                } else {
                    g.lineTo(x, y);
                }
            }
            g.endFill();
            if (verts.length > 2) {
                g.moveTo(verts[verts.length - 1][0], verts[verts.length - 1][1]);
                g.lineTo(verts[0][0], verts[0][1]);
            }
        } else {
            var colors = [0xff0000, 0x00ff00, 0x0000ff];
            for (var i = 0; i !== verts.length + 1; i++) {
                var v0 = verts[i % verts.length],
                    v1 = verts[(i + 1) % verts.length],
                    x0 = v0[0],
                    y0 = v0[1],
                    x1 = v1[0],
                    y1 = v1[1];
                g.lineStyle(lineWidth, colors[i % colors.length], 1);
                g.moveTo(x0, y0);
                g.lineTo(x1, y1);
                g.drawCircle(x0, y0, lineWidth * 2);
            }

            g.lineStyle(lineWidth, 0x000000, 1);
            g.drawCircle(offset[0], offset[1], lineWidth * 2);
        }
    };

    WebGLRenderer.prototype.drawPath = function (g, path, color, fillColor, lineWidth, isSleeping) {
        lineWidth = typeof(lineWidth) === "number" ? lineWidth : 1;
        color = typeof(color) === "undefined" ? 0x000000 : color;
        g.lineStyle(lineWidth, color, 1);
        if (typeof(fillColor) === "number") {
            g.beginFill(fillColor, isSleeping ? this.sleepOpacity : 1.0);
        }
        var lastx = null,
            lasty = null;
        for (var i = 0; i < path.length; i++) {
            var v = path[i],
                x = v[0],
                y = v[1];
            if (x !== lastx || y !== lasty) {
                if (i === 0) {
                    g.moveTo(x, y);
                } else {
                    // Check if the lines are parallel
                    var p1x = lastx,
                        p1y = lasty,
                        p2x = x,
                        p2y = y,
                        p3x = path[(i + 1) % path.length][0],
                        p3y = path[(i + 1) % path.length][1];
                    var area = ((p2x - p1x) * (p3y - p1y)) - ((p3x - p1x) * (p2y - p1y));
                    if (area !== 0) {
                        g.lineTo(x, y);
                    }
                }
                lastx = x;
                lasty = y;
            }
        }
        if (typeof(fillColor) === "number") {
            g.endFill();
        }

        // Close the path
        if (path.length > 2 && typeof(fillColor) === "number") {
            g.moveTo(path[path.length - 1][0], path[path.length - 1][1]);
            g.lineTo(path[0][0], path[0][1]);
        }
    };

    WebGLRenderer.prototype.updateSpriteTransform = function (sprite, body) {
        if (this.useInterpolatedPositions && !this.paused) {
            sprite.position.x = body.interpolatedPosition[0];
            sprite.position.y = body.interpolatedPosition[1];
            sprite.rotation = body.interpolatedAngle;
        } else {
            sprite.position.x = body.position[0];
            sprite.position.y = body.position[1];
            sprite.rotation = body.angle;
        }
    };

    var X = p2.vec2.fromValues(1, 0),
        distVec = p2.vec2.fromValues(0, 0),
        worldAnchorA = p2.vec2.fromValues(0, 0),
        worldAnchorB = p2.vec2.fromValues(0, 0);
    WebGLRenderer.prototype.render = function () {
        var w = this.renderer.width,
            h = this.renderer.height,
            springSprites = this.springSprites,
            sprites = this.sprites;

        // Update body transforms
        for (var i = 0; i !== this.bodies.length; i++) {
            this.updateSpriteTransform(this.sprites[i], this.bodies[i]);
        }

        // Update graphics if the body changed sleepState
        for (var i = 0; i !== this.bodies.length; i++) {
            var isSleeping = (this.bodies[i].sleepState === p2.Body.SLEEPING);
            var sprite = this.sprites[i];
            var body = this.bodies[i];
            if (sprite.drawnSleeping !== isSleeping) {
                sprite.clear();
                this.drawRenderable(body, sprite, sprite.drawnColor, sprite.drawnLineColor);
            }
        }

        // Update spring transforms
        for (var i = 0; i !== this.springs.length; i++) {
            var s = this.springs[i],
                sprite = springSprites[i],
                bA = s.bodyA,
                bB = s.bodyB;

            if (this.useInterpolatedPositions && !this.paused) {
                p2.vec2.toGlobalFrame(worldAnchorA, s.localAnchorA, bA.interpolatedPosition, bA.interpolatedAngle);
                p2.vec2.toGlobalFrame(worldAnchorB, s.localAnchorB, bB.interpolatedPosition, bB.interpolatedAngle);
            } else {
                s.getWorldAnchorA(worldAnchorA);
                s.getWorldAnchorB(worldAnchorB);
            }

            sprite.scale.y = 1;
            if (worldAnchorA[1] < worldAnchorB[1]) {
                var tmp = worldAnchorA;
                worldAnchorA = worldAnchorB;
                worldAnchorB = tmp;
                sprite.scale.y = -1;
            }

            var sxA = worldAnchorA[0],
                syA = worldAnchorA[1],
                sxB = worldAnchorB[0],
                syB = worldAnchorB[1];

            // Spring position is the mean point between the anchors
            sprite.position.x = ( sxA + sxB ) / 2;
            sprite.position.y = ( syA + syB ) / 2;

            // Compute distance vector between anchors, in screen coords
            distVec[0] = sxA - sxB;
            distVec[1] = syA - syB;

            // Compute angle
            sprite.rotation = Math.acos(p2.vec2.dot(X, distVec) / p2.vec2.length(distVec));

            // And scale
            sprite.scale.x = p2.vec2.length(distVec) / s.restLength;
        }

        // Clear contacts
        if (this.drawContacts) {
            this.contactGraphics.clear();
            this.stage.removeChild(this.contactGraphics);
            this.stage.addChild(this.contactGraphics);

            var g = this.contactGraphics;
            g.lineStyle(this.lineWidth, 0x000000, 1);
            for (var i = 0; i !== this.world.narrowphase.contactEquations.length; i++) {
                var eq = this.world.narrowphase.contactEquations[i],
                    bi = eq.bodyA,
                    bj = eq.bodyB,
                    ri = eq.contactPointA,
                    rj = eq.contactPointB,
                    xi = bi.position[0],
                    yi = bi.position[1],
                    xj = bj.position[0],
                    yj = bj.position[1];

                g.moveTo(xi, yi);
                g.lineTo(xi + ri[0], yi + ri[1]);

                g.moveTo(xj, yj);
                g.lineTo(xj + rj[0], yj + rj[1]);

            }
            this.contactGraphics.cleared = false;
        } else if (!this.contactGraphics.cleared) {
            this.contactGraphics.clear();
            this.contactGraphics.cleared = true;
        }

        // Draw AABBs
        if (this.drawAABBs) {
            this.aabbGraphics.clear();
            this.stage.removeChild(this.aabbGraphics);
            this.stage.addChild(this.aabbGraphics);
            var g = this.aabbGraphics;
            g.lineStyle(this.lineWidth, 0x000000, 1);

            for (var i = 0; i !== this.world.bodies.length; i++) {
                var aabb = this.world.bodies[i].getAABB();
                g.drawRect(aabb.lowerBound[0], aabb.lowerBound[1], aabb.upperBound[0] - aabb.lowerBound[0], aabb.upperBound[1] - aabb.lowerBound[1]);
            }
            this.aabbGraphics.cleared = false;
        } else if (!this.aabbGraphics.cleared) {
            this.aabbGraphics.clear();
            this.aabbGraphics.cleared = true;
        }

        if (this.followBody) {
            app.centerCamera(this.followBody.interpolatedPosition[0], this.followBody.interpolatedPosition[1]);
        }

        this.renderer.render(this.container);
    };

//http://stackoverflow.com/questions/5623838/rgb-to-hex-and-hex-to-rgb
    function componentToHex(c) {
        var hex = c.toString(16);
        return hex.length === 1 ? "0" + hex : hex;
    }

    function rgbToHex(r, g, b) {
        return componentToHex(r) + componentToHex(g) + componentToHex(b);
    }

//http://stackoverflow.com/questions/43044/algorithm-to-randomly-generate-an-aesthetically-pleasing-color-palette
    function randomPastelHex() {
        var mix = [255, 255, 255];
        var red = Math.floor(Math.random() * 256);
        var green = Math.floor(Math.random() * 256);
        var blue = Math.floor(Math.random() * 256);

        // mix the color
        red = Math.floor((red + 3 * mix[0]) / 4);
        green = Math.floor((green + 3 * mix[1]) / 4);
        blue = Math.floor((blue + 3 * mix[2]) / 4);

        return rgbToHex(red, green, blue);
    }

    WebGLRenderer.prototype.drawRenderable = function (obj, graphics, color, lineColor) {
        var lw = this.lineWidth;

        var zero = [0, 0];
        graphics.drawnSleeping = false;
        graphics.drawnColor = color;
        graphics.drawnLineColor = lineColor;

        if (obj instanceof p2.Body && obj.shapes.length) {

            var isSleeping = (obj.sleepState === p2.Body.SLEEPING);
            graphics.drawnSleeping = isSleeping;

            if (obj.concavePath && !this.debugPolygons) {
                var path = [];
                for (var j = 0; j !== obj.concavePath.length; j++) {
                    var v = obj.concavePath[j];
                    path.push([v[0], v[1]]);
                }
                this.drawPath(graphics, path, lineColor, color, lw, isSleeping);
            } else {
                for (var i = 0; i < obj.shapes.length; i++) {
                    var child = obj.shapes[i],
                        offset = child.position,
                        angle = child.angle;

                    if (child instanceof p2.Circle) {
                        this.drawCircle(graphics, offset[0], offset[1], angle, child.radius, color, lw, isSleeping);

                    } else if (child instanceof p2.Particle) {
                        this.drawCircle(graphics, offset[0], offset[1], angle, 2 * lw, lineColor, 0);

                    } else if (child instanceof p2.Plane) {
                        // TODO use shape angle
                        WebGLRenderer.drawPlane(graphics, -10, 10, color, lineColor, lw, lw * 10, lw * 10, 100);

                    } else if (child instanceof p2.Line) {
                        WebGLRenderer.drawLine(graphics, offset, angle, child.length, lineColor, lw);

                    } else if (child instanceof p2.Box) {
                        this.drawRectangle(graphics, offset[0], offset[1], angle, child.width, child.height, lineColor, color, lw, isSleeping);

                    } else if (child instanceof p2.Capsule) {
                        this.drawCapsule(graphics, offset[0], offset[1], angle, child.length, child.radius, lineColor, color, lw, isSleeping);

                    } else if (child instanceof p2.Convex) {
                        // Scale verts
                        var verts = [],
                            vrot = p2.vec2.create();
                        for (var j = 0; j !== child.vertices.length; j++) {
                            var v = child.vertices[j];
                            p2.vec2.rotate(vrot, v, angle);
                            verts.push([(vrot[0] + offset[0]), (vrot[1] + offset[1])]);
                        }
                        this.drawConvex(graphics, verts, child.triangles, lineColor, color, lw, this.debugPolygons, [offset[0], -offset[1]], isSleeping);

                    } else if (child instanceof p2.Heightfield) {
                        var path = [[0, -100]];
                        for (var j = 0; j !== child.heights.length; j++) {
                            var v = child.heights[j];
                            path.push([j * child.elementWidth, v]);
                        }
                        path.push([child.heights.length * child.elementWidth, -100]);
                        this.drawPath(graphics, path, lineColor, color, lw, isSleeping);

                    }
                }
            }

        } else if (obj instanceof p2.Spring) {
            var restLengthPixels = obj.restLength;
            WebGLRenderer.drawSpring(graphics, restLengthPixels, 0x000000, lw);
        }
    };

    WebGLRenderer.prototype.addRenderable = function (obj) {
        var lw = this.lineWidth;

        // Random color
        var color = parseInt(randomPastelHex(), 16),
            lineColor = 0x000000;

        var zero = [0, 0];

        var sprite = new PIXI.Graphics();
        if (obj instanceof p2.Body && obj.shapes.length) {

            this.drawRenderable(obj, sprite, color, lineColor);
            this.sprites.push(sprite);
            this.stage.addChild(sprite);

        } else if (obj instanceof p2.Spring) {
            this.drawRenderable(obj, sprite, 0x000000, lineColor);
            this.springSprites.push(sprite);
            this.stage.addChild(sprite);
        }
    };

    WebGLRenderer.prototype.removeRenderable = function (obj) {
        if (obj instanceof p2.Body) {
            var i = this.bodies.indexOf(obj);
            if (i !== -1) {
                this.stage.removeChild(this.sprites[i]);
                this.sprites.splice(i, 1);
            }
        } else if (obj instanceof p2.Spring) {
            var i = this.springs.indexOf(obj);
            if (i !== -1) {
                this.stage.removeChild(this.springSprites[i]);
                this.springSprites.splice(i, 1);
            }
        }
    };

    WebGLRenderer.prototype.resize = function (w, h) {
        var renderer = this.renderer;
        var view = renderer.view;
        var ratio = w / h;
        renderer.resize(w, h);
    };
}