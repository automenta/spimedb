/* global PIXI,Renderer */

module.exports = function (p2, targetFPS) {

    p2.StateMachine = StateMachine;

    /**
     * var fsm = new StateMachine({
 *     states: [
 *         1, 2, 3 // first one is the initial state
 *     ],
 *     transitions: [
 *         [1, 2],
 *         [2, 3],
 *         [3, 1]
 *     ]
 * });
     */
    function StateMachine(options){
        p2.EventEmitter.call(this);

        this.states = options.states.slice(0);
        this.state = this.states[0];
        this.transitions = [];
        for(var i=0; i < options.transitions.length; i++){
            this.transitions.push([
                options.transitions[i][0],
                options.transitions[i][1]
            ]);
        }
    }
    StateMachine.prototype = Object.create(p2.EventEmitter.prototype);

    StateMachine.prototype.transitionTo = function(toState){
        if(this.state === toState){
            return;
        }

        // Check if OK
        var ok = false;
        for(var i=0; i<this.transitions.length; i++){
            if(this.transitions[i][0] === this.state && this.transitions[i][1] === toState){
                ok = true;
                break;
            }
        }

        if(!ok){
            throw new Error('Illegal transition from ' + this.state + ' to ' + toState + '.');
        }

        this.state = toState;
        this.emit({
            state: toState
        });
        this.onStateChanged(toState);

        return this;
    };

// To be implemented by subclasses
    StateMachine.prototype.onStateChanged = function(){};

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
    var vec2 = p2.vec2;

    /**
     * Base class for rendering a p2 physics scene.
     * @class Renderer
     * @constructor
     * @param {object} scenes One or more scene definitions. See setScene.
     */
    function Renderer(scenes, options){
        p2.StateMachine.call(this, {
            states: [
                Renderer.DEFAULT,
                Renderer.PANNING,
                Renderer.DRAGGING,
                Renderer.DRAWPOLYGON,
                Renderer.DRAWINGPOLYGON,
                Renderer.DRAWCIRCLE,
                Renderer.DRAWINGCIRCLE,
                Renderer.DRAWRECTANGLE,
                Renderer.DRAWINGRECTANGLE
            ],
            transitions: [
                [Renderer.DEFAULT, Renderer.PANNING],
                [Renderer.PANNING, Renderer.DEFAULT],

                [Renderer.DEFAULT, Renderer.DRAGGING],
                [Renderer.DRAGGING, Renderer.DEFAULT],

                [Renderer.DEFAULT, Renderer.DRAWPOLYGON],
                [Renderer.DRAWPOLYGON, Renderer.DEFAULT],
                [Renderer.DRAWPOLYGON, Renderer.DRAWINGPOLYGON],
                [Renderer.DRAWINGPOLYGON, Renderer.DRAWPOLYGON],

                [Renderer.DEFAULT, Renderer.DRAWCIRCLE],
                [Renderer.DRAWCIRCLE, Renderer.DEFAULT],
                [Renderer.DRAWCIRCLE, Renderer.DRAWINGCIRCLE],
                [Renderer.DRAWINGCIRCLE, Renderer.DRAWCIRCLE],

                [Renderer.DEFAULT, Renderer.DRAWRECTANGLE],
                [Renderer.DRAWRECTANGLE, Renderer.DEFAULT],
                [Renderer.DRAWRECTANGLE, Renderer.DRAWINGRECTANGLE],
                [Renderer.DRAWINGRECTANGLE, Renderer.DRAWRECTANGLE]
            ]
        });

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
        this.bodyPolygonPaths = {}; // body id -> array<vec2>
        this.state = Renderer.DEFAULT;

        this.zoom = 200; // pixels per unit

        this.cameraPosition = vec2.create();

        // Bodies to draw
        this.bodies=[];
        this.springs=[];
        this.timeStep = 1/60;
        this.relaxation = p2.Equation.DEFAULT_RELAXATION;
        this.stiffness = p2.Equation.DEFAULT_STIFFNESS;

        this.mouseConstraint = null;
        this.nullBody = new p2.Body();
        this.pickPrecision = 0.1;

        this.useInterpolatedPositions = true;

        this.drawPoints = [];
        this.drawPointsChangeEvent = { type : "drawPointsChange" };
        this.drawCircleCenter = vec2.create();
        this.drawCirclePoint = vec2.create();
        this.drawCircleChangeEvent = { type : "drawCircleChange" };
        this.drawRectangleChangeEvent = { type : "drawRectangleChange" };
        this.drawRectStart = vec2.create();
        this.drawRectEnd = vec2.create();

        this.stateChangeEvent = { type : "stateChange", state:null };

        this.mousePosition = vec2.create();

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

        this.setCameraCenter([0, 0]);

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
    Renderer.prototype = Object.create(p2.StateMachine.prototype);

// States
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

    Object.defineProperties(Renderer.prototype, {

        drawContacts: {
            get: function() {
                return this.settings['drawContacts [c]'];
            },
            set: function(value) {
                this.settings['drawContacts [c]'] = value;
                this.updateGUI();
            }
        },

        drawAABBs: {
            get: function() {
                return this.settings['drawAABBs [t]'];
            },
            set: function(value) {
                this.settings['drawAABBs [t]'] = value;
                this.updateGUI();
            }
        },

        paused: {
            get: function() {
                return this.settings['paused [p]'];
            },
            set: function(value) {
                this.resetCallTime = true;
                this.settings['paused [p]'] = value;
                this.updateGUI();
            }
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
            that.transitionTo(Renderer.DEFAULT).transitionTo(parseInt(state));
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
                vec2.set(that.world.gravity, settings.gravityX, settings.gravityY);
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

        world.on("postStep",function(/*e*/){
            that.updateStats();
        }).on("addBody",function(e){
            that.addVisual(e.body);
        }).on("removeBody",function(e){
            that.removeVisual(e.body);
            delete that.bodyPolygonPaths[e.body.id];
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
            delete window[this.addedGlobals[i]];
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
                    that.transitionTo(Renderer.DEFAULT);
                    that.transitionTo(s === Renderer.DRAWPOLYGON ? Renderer.DEFAULT : s = Renderer.DRAWPOLYGON);
                    break;
                case "A": // toggle draw circle mode
                    that.transitionTo(Renderer.DEFAULT);
                    that.transitionTo(s === Renderer.DRAWCIRCLE ? Renderer.DEFAULT : s = Renderer.DRAWCIRCLE);
                    break;
                case "F": // toggle draw rectangle mode
                    that.transitionTo(Renderer.DEFAULT);
                    that.transitionTo(s === Renderer.DRAWRECTANGLE ? Renderer.DEFAULT : s = Renderer.DRAWRECTANGLE);
                    break;
                case "Q": // set default
                    that.transitionTo(Renderer.DEFAULT);
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
                    that.emit({
                        type: "keydown",
                        originalEvent: e,
                        keyCode: e.keyCode
                    });
                    break;
            }
            that.updateGUI();
        };

        this.elementContainer.onkeyup = function(e){
            if(e.keyCode){
                switch(String.fromCharCode(e.keyCode)){
                    default:
                        that.emit({
                            type: "keyup",
                            originalEvent: e,
                            keyCode: e.keyCode
                        });
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

        var lastTime;

        function update(time){
            if(!demo.paused){
                var now = Date.now() / 1000,
                    timeSinceLastCall = now - lastCallTime;
                if(demo.resetCallTime){
                    timeSinceLastCall = 0;
                    demo.resetCallTime = false;
                }
                lastCallTime = now;

                // Cap if we have a really large deltatime.
                // The requestAnimationFrame deltatime is usually below 0.0333s (30Hz) and on desktops it should be below 0.0166s.
                timeSinceLastCall = Math.min(timeSinceLastCall, 0.5);

                demo.world.step(demo.timeStep, timeSinceLastCall, demo.settings.maxSubSteps);
            }

            var deltaTime = lastTime ? (time - lastTime) / 1000 : 0;
            lastTime = time;
            demo.render(deltaTime);

            requestAnimFrame(update);
        }
        requestAnimFrame(update);
    };

    /**
     * Set the app state.
     * @param {number} state
     */
    Renderer.prototype.onStateChanged = function(state){
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
                    this.transitionTo(Renderer.DRAGGING);
                    // Add mouse joint to the body
                    var localPoint = vec2.create();
                    b.toLocalFrame(localPoint,physicsPosition);
                    this.world.addBody(this.nullBody);
                    this.mouseConstraint = new p2.RevoluteConstraint(this.nullBody, b, {
                        localPivotA: physicsPosition,
                        localPivotB: localPoint,
                        maxForce: 1000 * b.mass
                    });
                    this.world.addConstraint(this.mouseConstraint);
                } else {
                    this.transitionTo(Renderer.PANNING);
                }
                break;

            case Renderer.DRAWPOLYGON:
                // Start drawing a polygon
                this.transitionTo(Renderer.DRAWINGPOLYGON);
                this.drawPoints = [];
                var copy = vec2.create();
                vec2.copy(copy,physicsPosition);
                this.drawPoints.push(copy);
                this.emit(this.drawPointsChangeEvent);
                break;

            case Renderer.DRAWCIRCLE:
                // Start drawing a circle
                this.transitionTo(Renderer.DRAWINGCIRCLE);
                vec2.copy(this.drawCircleCenter,physicsPosition);
                vec2.copy(this.drawCirclePoint, physicsPosition);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWRECTANGLE:
                // Start drawing a circle
                this.transitionTo(Renderer.DRAWINGRECTANGLE);
                vec2.copy(this.drawRectStart,physicsPosition);
                vec2.copy(this.drawRectEnd, physicsPosition);
                this.emit(this.drawRectangleChangeEvent);
                break;
        }
    };

    /**
     * Should be called by subclasses whenever there's a mousemove event
     */
    Renderer.prototype.handleMouseMove = function(physicsPosition){
        vec2.copy(this.mousePosition, physicsPosition);

        var sampling = 0.4;
        switch(this.state){
            case Renderer.DEFAULT:
            case Renderer.DRAGGING:
                if(this.mouseConstraint){
                    vec2.copy(this.mouseConstraint.pivotA, physicsPosition);
                    this.mouseConstraint.bodyA.wakeUp();
                    this.mouseConstraint.bodyB.wakeUp();
                }
                break;

            case Renderer.DRAWINGPOLYGON:
                // drawing a polygon - add new point
                var sqdist = vec2.distance(physicsPosition,this.drawPoints[this.drawPoints.length-1]);
                if(sqdist > sampling*sampling){
                    var copy = [0,0];
                    vec2.copy(copy,physicsPosition);
                    this.drawPoints.push(copy);
                    this.emit(this.drawPointsChangeEvent);
                }
                break;

            case Renderer.DRAWINGCIRCLE:
                // drawing a circle - change the circle radius point to current
                vec2.copy(this.drawCirclePoint, physicsPosition);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWINGRECTANGLE:
                // drawing a rectangle - change the end point to current
                vec2.copy(this.drawRectEnd, physicsPosition);
                this.emit(this.drawRectangleChangeEvent);
                break;
        }
    };

    /**
     * Should be called by subclasses whenever there's a mouseup event
     */
    Renderer.prototype.handleMouseUp = function(/*physicsPosition*/){

        var b;

        switch(this.state){

            case Renderer.DEFAULT:
                break;

            case Renderer.DRAGGING:
                // Drop constraint
                this.world.removeConstraint(this.mouseConstraint);
                this.mouseConstraint = null;
                this.world.removeBody(this.nullBody);
                this.transitionTo(Renderer.DEFAULT);
                break;

            case Renderer.PANNING:
                this.transitionTo(Renderer.DEFAULT);
                break;

            case Renderer.DRAWINGPOLYGON:
                // End this drawing state
                this.transitionTo(Renderer.DRAWPOLYGON);
                if(this.drawPoints.length > 3){
                    // Create polygon
                    b = new p2.Body({ mass: 1 });
                    if (b.fromPolygon(this.drawPoints, { removeCollinearPoints: 0.1 })) {
                        var bodyPath = this.bodyPolygonPaths[b.id] = [];
                        for(var i=0; i<this.drawPoints.length; i++){
                            var point = vec2.clone(this.drawPoints[i]);
                            vec2.subtract(point, point, b.position); // .fromPolygon() will move the body to the center of mass. Compensate by doing this.
                            bodyPath.push(point);
                        }
                        this.world.addBody(b);
                    }
                }
                this.drawPoints.length = 0;
                this.emit(this.drawPointsChangeEvent);
                break;

            case Renderer.DRAWINGCIRCLE:
                // End this drawing state
                this.transitionTo(Renderer.DRAWCIRCLE);
                var R = vec2.distance(this.drawCircleCenter,this.drawCirclePoint);
                if(R > 0){
                    // Create circle
                    b = new p2.Body({ mass : 1, position : this.drawCircleCenter });
                    var circle = new p2.Circle({ radius: R });
                    b.addShape(circle);
                    this.world.addBody(b);
                }
                vec2.copy(this.drawCircleCenter,this.drawCirclePoint);
                this.emit(this.drawCircleChangeEvent);
                break;

            case Renderer.DRAWINGRECTANGLE:
                // End this drawing state
                this.transitionTo(Renderer.DRAWRECTANGLE);
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
                vec2.copy(this.drawRectEnd,this.drawRectStart);
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

    Renderer.prototype.setEquationParameters = function(){
        this.world.setGlobalStiffness(this.settings.stiffness);
        this.world.setGlobalRelaxation(this.settings.relaxation);
    };

// Set camera position in physics space
    Renderer.prototype.setCameraCenter = function(position){
        vec2.set(this.cameraPosition, position[0], position[1]);
        this.onCameraPositionChanged();
    };

// Set camera zoom level
    Renderer.prototype.setZoom = function(zoom){
        this.zoom = zoom;
        this.onZoomChanged();
    };

// Zoom around a point
    Renderer.prototype.zoomAroundPoint = function(point, deltaZoom){
        this.zoom *= 1 + deltaZoom;

        // Move the camera closer to the zoom point, if the delta is positive
        // If delta is infinity, the camera position should go toward the point
        // If delta is close to zero, the camera position should be almost unchanged
        // p = (point + p * delta) / (1 + delta)
        vec2.set(
            this.cameraPosition,
            (this.cameraPosition[0] + point[0] * deltaZoom) / (1 + deltaZoom),
            (this.cameraPosition[1] + point[1] * deltaZoom) / (1 + deltaZoom)
        );
        this.onZoomChanged();
        this.onCameraPositionChanged();
    };

    Renderer.prototype.onCameraPositionChanged = function(){};
    Renderer.prototype.onZoomChanged = function(){};

    p2.WebGLRenderer = WebGLRenderer;

    var vec2 = p2.vec2;
    var Renderer = p2.Renderer;

    function copyPixiVector(out, pixiVector){
        if (!out)
            return; //TODO detect this?

        out[0] = pixiVector.x;
        out[1] = pixiVector.y;
    }

    function smoothDamp(current, target, store, deltaTime, smoothTime, maxSpeed) {
        if (smoothTime === undefined) {
            smoothTime = 0.3;
        }
        if (maxSpeed === undefined) {
            maxSpeed = 1e7;
        }

        smoothTime = Math.max(0.0001, smoothTime);
        var num = 2 / smoothTime;
        var num2 = num * deltaTime;
        var num3 = 1 / (1 + num2 + 0.48 * num2 * num2 + 0.235 * num2 * num2 * num2);
        var num4 = current - target;
        var num5 = target;
        var num6 = maxSpeed * smoothTime;
        num4 = Math.max(-num6, Math.min(num6, num4));
        target = current - num4;
        var currentVelocity = store.y;
        var num7 = (currentVelocity + num * num4) * deltaTime;
        currentVelocity = (currentVelocity - num * num7) * num3;
        var num8 = target + (num4 + num7) * num3;
        if (num5 - current > 0 === num8 > num5){
            num8 = num5;
            currentVelocity = (num8 - num5) / deltaTime;
        }
        store.x = num8;
        store.y = currentVelocity;

        return num8;
    }

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
    function WebGLRenderer(scenes, options){
        options = options || {};

        var that = this;

        var settings = {
            lineColor: 0x000000,
            lineWidth : 0.01,
            scrollFactor : 0.1,
            width : 1280, // Pixi screen resolution
            height : 720,
            useDeviceAspect : false,
            sleepOpacity : 0.2
        };
        for(var key in options){
            settings[key] = options[key];
        }

        if(settings.useDeviceAspect){
            settings.height = window.innerHeight / window.innerWidth * settings.width;
        }

        this.lineWidth =            settings.lineWidth;
        this.lineColor =            settings.lineColor;
        this.drawLineColor =        0xff0000;
        this.scrollFactor =         settings.scrollFactor;
        this.sleepOpacity =         settings.sleepOpacity;

        // Camera smoothing settings
        this.smoothTime = 0.03;
        this.maxSmoothVelocity = 1e7;

        this.sprites = [];
        this.springSprites = [];
        this.debugPolygons = false;

        this.islandColors = {}; // id -> int
        this.islandStaticColor = 0xdddddd;

        this.startMouseDelta = vec2.create();
        this.startCamPos = vec2.create();

        this.touchPositions = {}; // identifier => vec2 in physics space
        this.mouseDown = false;

        this._zoomStore = {};
        this._cameraStoreX = {};
        this._cameraStoreY = {};

        Renderer.call(this,scenes,options);

        // Camera smoothing state
        this.currentZoom = this.zoom;
        this.currentStageX = 0;
        this.currentStageY = 0;

        for(var key in settings){
            this.settings[key] = settings[key];
        }

        // Update "ghost draw line"
        this.on("drawPointsChange",function(/*e*/){
            var g = that.drawShapeGraphics;
            var path = that.drawPoints;

            if(!g.parent){
                that.stage.addChild(g);
            }

            g.clear();

            if(!path.length){
                that.stage.removeChild(g);
                return;
            }

            var path2 = [];
            for(var j=0; j<path.length; j++){
                var v = path[j];
                path2.push([v[0], v[1]]);
            }

            that.drawPath(g, path2, that.lineWidth, that.drawLineColor, that.lineColor, 0);
        });

        // Update draw circle
        this.on("drawCircleChange",function(/*e*/){
            var g = that.drawShapeGraphics;
            if(!g.parent){
                that.stage.addChild(g);
            }
            g.clear();
            var tmpCircle = new p2.Circle({
                radius: vec2.distance(that.drawCircleCenter, that.drawCirclePoint),
                position: that.drawCircleCenter
            });
            that.drawCircle(g, tmpCircle, 0x000000, 0, that.lineWidth);
        });

        // Update draw circle
        this.on("drawRectangleChange",function(/*e*/){
            var g = that.drawShapeGraphics;
            if(!g.parent){
                that.stage.addChild(g);
            }
            g.clear();
            var start = that.drawRectStart;
            var end = that.drawRectEnd;
            var w = start[0] - end[0];
            var h = start[1] - end[1];
            var tmpBox = new p2.Box({
                width: Math.abs(w),
                height: Math.abs(h),
                position: [
                    start[0] - w/2,
                    start[1] - h/2
                ]
            });
            that.drawRectangle(g, tmpBox, 0, 0, that.lineWidth);
        });
    }
    WebGLRenderer.prototype = Object.create(Renderer.prototype);

    /**
     * Initialize the renderer and stage
     */
    var init_physicsPosition = vec2.create();
    WebGLRenderer.prototype.init = function(){
        var s = this.settings;

        var that = this;

        this.renderer =     PIXI.autoDetectRenderer(s.width, s.height, { backgroundColor: 0xFFFFFF });
        var stage =     this.stage =        new PIXI.Container();
        var container = this.container =    new PIXI.Container();
        container.interactive = stage.interactive = true;

        container.hitArea = new PIXI.Rectangle(0,0,1e7,1e7);

        var el = this.element = this.renderer.view;
        el.tabIndex = 1;
        el.classList.add(Renderer.elementClass);
        el.setAttribute('style','width:100%;');

        var div = this.elementContainer = document.createElement('div');
        div.classList.add(Renderer.containerClass);
        div.setAttribute('style','width:100%; height:100%');
        div.appendChild(el);
        document.body.appendChild(div);
        el.focus();
        el.oncontextmenu = function(){
            return false;
        };

        this.container.addChild(stage);

        // Graphics object for drawing shapes
        this.drawShapeGraphics = new PIXI.Graphics();

        // Graphics object for contacts
        this.contactGraphics = new PIXI.Graphics();

        // Graphics object for AABBs
        this.aabbGraphics = new PIXI.Graphics();

        // Graphics object for pick
        this.pickGraphics = new PIXI.Graphics();

        stage.scale.set(this.zoom, -this.zoom); // Flip Y direction since pixi has down as Y axis

        var physicsPosA = vec2.create();
        var physicsPosB = vec2.create();
        var initPinchCenter = vec2.create();
        var touchPanDelta = vec2.create();
        var lastPinchLength = 0;
        var startZoom = 1;

        container.mousedown = container.touchstart = function(e){

            // store touch state
            if(e.data.identifier !== undefined){
                var touchPos = vec2.create();
                copyPixiVector(touchPos, e.data.getLocalPosition(stage));
                that.touchPositions[e.data.identifier] = touchPos;
            }

            if(e.data.originalEvent.touches && e.data.originalEvent.touches.length === 2){
                var pos = new PIXI.Point();
                var touchA = e.data.originalEvent.touches[0];
                var touchB = e.data.originalEvent.touches[1];

                e.data.getLocalPosition(stage, pos, new PIXI.Point(touchA.clientX * that.getDevicePixelRatio(), touchA.clientY * that.getDevicePixelRatio()));
                copyPixiVector(physicsPosA, pos);

                e.data.getLocalPosition(stage, pos, new PIXI.Point(touchB.clientX * that.getDevicePixelRatio(), touchB.clientY * that.getDevicePixelRatio()));
                copyPixiVector(physicsPosB, pos);

                lastPinchLength = vec2.distance(physicsPosA, physicsPosB);
                startZoom = that.zoom;
                vec2.add(initPinchCenter, physicsPosA, physicsPosB);
                vec2.scale(initPinchCenter, initPinchCenter, 0.5);

                return;
            }

            that.mouseDown = true;

            var pos = e.data.getLocalPosition(stage);
            copyPixiVector(init_physicsPosition, pos);
            that.handleMouseDown(init_physicsPosition);

            copyPixiVector(that.startMouseDelta, e.data.global);
            vec2.copy(that.startCamPos, that.cameraPosition);
        };

        container.mousemove = container.touchmove = function(e){

            // store touch state
            if(e.data.identifier !== undefined){
                copyPixiVector(that.touchPositions[e.data.identifier], e.data.getLocalPosition(stage));
            }

            var numTouchesDown = that.getNumTouches();

            if(numTouchesDown === 2){
                var physicsPosA = that.getTouchPosition(0);
                var physicsPosB = that.getTouchPosition(1);
                var pinchLength = vec2.distance(physicsPosA, physicsPosB);

                // Get center
                var center = vec2.create();
                vec2.add(center, physicsPosA, physicsPosB);
                vec2.scale(center, center, 0.5);

                that.setZoom(
                    startZoom * pinchLength / lastPinchLength
                    /*center,
                    (pinchLength - lastPinchLength) / (that.renderer.height / that.zoom) * 2*/
                );
                //lastPinchLength = pinchLength;

                vec2.subtract(touchPanDelta, initPinchCenter, center);
                vec2.add(touchPanDelta, touchPanDelta, that.cameraPosition);

                //that.setCameraCenter(touchPanDelta);

                return;
            }

            if((that.mouseDown || numTouchesDown !== 0) && that.state === Renderer.PANNING){
                var delta = vec2.create();
                var currentMousePosition = vec2.create();
                copyPixiVector(currentMousePosition, e.data.global);
                vec2.subtract(delta, currentMousePosition, that.startMouseDelta);
                that.domVectorToPhysics(delta, delta);

                // When we move mouse up, camera should go down
                vec2.scale(delta, delta, -1);

                // Add delta to the camera position where the panning started
                vec2.add(delta, delta, that.startCamPos);

                // Set new camera position
                that.setCameraCenter(delta);
            }

            var pos = e.data.getLocalPosition(stage);
            copyPixiVector(init_physicsPosition, pos);
            that.handleMouseMove(init_physicsPosition);
        };

        container.mouseup = function(e){
            that.mouseDown = false;
            var pos = e.data.getLocalPosition(stage);
            copyPixiVector(init_physicsPosition, pos);

            that.handleMouseUp(init_physicsPosition);
        };

        container.touchend = function(e){
            delete that.touchPositions[e.data.identifier];
            var pos = e.data.getLocalPosition(stage);
            copyPixiVector(init_physicsPosition, pos);

            that.handleMouseUp(init_physicsPosition);
        };

        // http://stackoverflow.com/questions/7691551/touchend-event-in-ios-webkit-not-firing
        this.element.ontouchmove = function(e){
            e.preventDefault();
        };

        function MouseWheelHandler(e) {
            // cross-browser wheel delta
            e = window.event || e; // old IE support

            var o = e,
                d = o.detail, w = o.wheelDelta,
                n = 225, n1 = n-1;

            // Normalize delta: http://stackoverflow.com/a/13650579/2285811
            var f;
            d = d ? w && (f = w/d) ? d/f : -d/1.35 : w/120;
            // Quadratic scale if |d| > 1
            d = d < 1 ? d < -1 ? (-Math.pow(d, 2) - n1) / n : d : (Math.pow(d, 2) + n1) / n;
            // Delta *should* not be greater than 2...
            var delta = Math.min(Math.max(d / 2, -1), 1);

            if(delta){
                var point = that.domToPhysics([e.clientX, e.clientY]);
                that.zoomAroundPoint(point, delta > 0 ? 0.05 : -0.05);
            }
        }

        if (el.addEventListener) {
            el.addEventListener("mousewheel", MouseWheelHandler, false); // IE9, Chrome, Safari, Opera
            el.addEventListener("DOMMouseScroll", MouseWheelHandler, false); // Firefox
        } else {
            el.attachEvent("onmousewheel", MouseWheelHandler); // IE 6/7/8
        }

        this.setCameraCenter([0, 0]);
    };

    WebGLRenderer.prototype.getNumTouches = function(){
        var touchPositions = this.touchPositions;
        var touchIdentifiers = Object.keys(touchPositions);
        var numTouchesDown = 0;
        for(var i=0; i<touchIdentifiers.length; i++){
            if(touchPositions[touchIdentifiers[i]]){
                numTouchesDown++;
            }
        }
        return numTouchesDown;
    };

    WebGLRenderer.prototype.getTouchPosition = function(i){
        var touchPositions = this.touchPositions;
        var touchIdentifiers = Object.keys(touchPositions);
        return touchPositions[touchIdentifiers[i]];
    };

    WebGLRenderer.prototype.domToPhysics = function(point){
        var result = this.stage.toLocal(new PIXI.Point(point[0], point[1]));
        return [result.x, result.y];
    };

    WebGLRenderer.prototype.domVectorToPhysics = function(vector, result){
        result[0] = vector[0] / this.zoom;
        result[1] = -vector[1] / this.zoom;
    };

    WebGLRenderer.prototype.onCameraPositionChanged = function(){
        // TODO: can this be simplified by adding another PIXI.Container?
        /*
        this.stage.position.set(
            this.renderer.width / 2 - this.stage.scale.x * this.cameraPosition[0],
            this.renderer.height / 2 - this.stage.scale.y * this.cameraPosition[1]
        );
        this.stage.updateTransform();
        */
    };

    WebGLRenderer.prototype.onZoomChanged = function(){
        /*
        this.stage.scale.set(this.zoom, -this.zoom);
        this.stage.updateTransform();
        */
    };

    /**
     * Make sure that a rectangle is visible in the canvas.
     * @param  {number} centerX
     * @param  {number} centerY
     * @param  {number} width
     * @param  {number} height
     */
    WebGLRenderer.prototype.frame = function(centerX, centerY, width, height){
        var renderer = this.renderer;
        this.setCameraCenter([centerX, centerY]);
        var ratio = renderer.width / renderer.height;

        var zoom;
        if(ratio < width / height){
            zoom = renderer.width / width;
        } else {
            zoom = renderer.height / height;
        }
        this.setZoom(zoom);
    };

    function pixiDrawCircleOnGraphics(g, x, y, radius){
        var maxSegments = 32;
        var minSegments = 12;

        var alpha = (radius - 0.1) / 1;
        alpha = Math.max(Math.min(alpha, 1), 0);

        var numSegments = Math.round(
            maxSegments * alpha + minSegments * (1-alpha)
        );

        var circlePath = [];
        for(var i=0; i<numSegments+1; i++){
            var a = Math.PI * 2 / numSegments * i;
            circlePath.push(
                new PIXI.Point(
                    x + radius * Math.cos(a),
                    y + radius * Math.sin(a)
                )
            );
        }

        g.drawPolygon(circlePath);
    }

    WebGLRenderer.prototype.drawSpring = function(g,restLength,color,lineWidth){
        g.lineStyle(lineWidth, color, 1);
        if(restLength < lineWidth*10){
            restLength = lineWidth*10;
        }
        var M = 12;
        var dx = restLength/M;
        g.moveTo(-restLength/2,0);
        for(var i=1; i<M; i++){
            var x = -restLength/2 + dx*i;
            var y = 0;
            if(i<=1 || i>=M-1 ){
                // Do nothing
            } else if(i % 2 === 0){
                y -= 0.1*restLength;
            } else {
                y += 0.1*restLength;
            }
            g.lineTo(x,y);
        }
        g.lineTo(restLength/2,0);
    };

    WebGLRenderer.prototype.shapeDrawFunctions = {};

    var tmpCircle = new p2.Circle({ radius: 0.02 });
    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.PARTICLE] =
        WebGLRenderer.prototype.drawParticle = function(graphics, particleShape, color, alpha, lineWidth){
            this.drawCircle(graphics, tmpCircle, this.lineColor, 1, 0);
        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.CIRCLE] =
        WebGLRenderer.prototype.drawCircle = function(g, circleShape, color, alpha, lineWidth){
            g.lineStyle(lineWidth, this.lineColor, 1);
            g.beginFill(color, alpha);
            var x = circleShape.position[0];
            var y = circleShape.position[1];
            var r = circleShape.radius;
            pixiDrawCircleOnGraphics(g, x, y, r, 20);
            g.endFill();

            // line from center to edge
            g.moveTo(x,y);
            g.lineTo(   x + r * Math.cos(circleShape.angle),
                y + r * Math.sin(circleShape.angle) );
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
    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.PLANE] =
        WebGLRenderer.prototype.drawPlane = function(g, planeShape, color, alpha, lineWidth){
            g.lineStyle(lineWidth, this.lineColor, 1);

            // Draw a fill color
            g.lineStyle(0,0,0);
            g.beginFill(color);
            var max = 100;
            g.moveTo(-max,0);
            g.lineTo(max,0);
            g.lineTo(max,-max);
            g.lineTo(-max,-max);
            g.endFill();

            // Draw the actual plane
            g.lineStyle(lineWidth, this.lineColor);
            g.moveTo(-max,0);
            g.lineTo(max,0);
        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.LINE] =
        WebGLRenderer.prototype.drawLine = function(g, shape, color, alpha, lineWidth){
            var len = shape.length;
            var angle = shape.angle;
            var offset = shape.position;

            g.lineStyle(lineWidth, color, 1);

            var startPoint = vec2.fromValues(-len/2,0);
            var endPoint = vec2.fromValues(len/2,0);

            vec2.rotate(startPoint, startPoint, angle);
            vec2.rotate(endPoint, endPoint, angle);

            vec2.add(startPoint, startPoint, offset);
            vec2.add(endPoint, endPoint, offset);

            g.moveTo(startPoint[0], startPoint[1]);
            g.lineTo(endPoint[0], endPoint[1]);
        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.CAPSULE] =
        WebGLRenderer.prototype.drawCapsule = function(g, shape, color, alpha, lineWidth){
            var angle = shape.angle;
            var radius = shape.radius;
            var len = shape.length;
            var x = shape.position[0];
            var y = shape.position[1];

            g.lineStyle(lineWidth, this.lineColor, 1);

            // Draw circles at ends
            var hl = len / 2;
            g.beginFill(color, alpha);
            var localPos = vec2.fromValues(x, y);
            var p0 = vec2.fromValues(-hl, 0);
            var p1 = vec2.fromValues(hl, 0);
            vec2.rotate(p0, p0, angle);
            vec2.rotate(p1, p1, angle);
            vec2.add(p0, p0, localPos);
            vec2.add(p1, p1, localPos);
            pixiDrawCircleOnGraphics(g, p0[0], p0[1], radius, 20);
            pixiDrawCircleOnGraphics(g, p1[0], p1[1], radius, 20);
            g.endFill();

            // Draw rectangle
            var pp2 = vec2.create();
            var p3 = vec2.create();
            vec2.set(p0, -hl, radius);
            vec2.set(p1, hl, radius);
            vec2.set(pp2, hl, -radius);
            vec2.set(p3, -hl, -radius);

            vec2.rotate(p0, p0, angle);
            vec2.rotate(p1, p1, angle);
            vec2.rotate(pp2, pp2, angle);
            vec2.rotate(p3, p3, angle);

            vec2.add(p0, p0, localPos);
            vec2.add(p1, p1, localPos);
            vec2.add(pp2, pp2, localPos);
            vec2.add(p3, p3, localPos);

            g.lineStyle(lineWidth, this.lineColor, 0);
            g.beginFill(color, alpha);
            g.moveTo(p0[0], p0[1]);
            g.lineTo(p1[0], p1[1]);
            g.lineTo(pp2[0], pp2[1]);
            g.lineTo(p3[0], p3[1]);
            g.endFill();

            // Draw lines in between
            for(var i=0; i<2; i++){
                g.lineStyle(lineWidth, this.lineColor, 1);
                var sign = (i===0?1:-1);
                vec2.set(p0, -hl, sign*radius);
                vec2.set(p1, hl, sign*radius);
                vec2.rotate(p0, p0, angle);
                vec2.rotate(p1, p1, angle);
                vec2.add(p0, p0, localPos);
                vec2.add(p1, p1, localPos);
                g.moveTo(p0[0], p0[1]);
                g.lineTo(p1[0], p1[1]);
            }

        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.BOX] =
        WebGLRenderer.prototype.drawRectangle = function(g, shape, color, alpha, lineWidth){
            var w = shape.width;
            var h = shape.height;
            var x = shape.position[0];
            var y = shape.position[1];

            var path = [
                [w / 2, h / 2],
                [-w / 2, h / 2],
                [-w / 2, -h / 2],
                [w / 2, -h / 2]
            ];

            // Rotate and add position
            for (var i = 0; i < path.length; i++) {
                var v = path[i];
                vec2.rotate(v, v, shape.angle);
                vec2.add(v, v, [x, y]);
            }

            this.drawPath(g, path, lineWidth, this.lineColor, color, alpha);
        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.HEIGHTFIELD] =
        WebGLRenderer.prototype.drawHeightfield = function(g, shape, color, alpha, lineWidth){
            var path = [[0,-100]];
            for(var j=0; j!==shape.heights.length; j++){
                var v = shape.heights[j];
                path.push([j*shape.elementWidth, v]);
            }
            path.push([shape.heights.length * shape.elementWidth, -100]);
            this.drawPath(g, path, lineWidth, this.lineColor, color, alpha);
        };

    WebGLRenderer.prototype.shapeDrawFunctions[p2.Shape.CONVEX] =
        WebGLRenderer.prototype.drawConvex = function(g, shape, color, alpha, lineWidth){
            var verts = [];
            var vrot = vec2.create();
            var offset = shape.position;

            for(var j=0; j!==shape.vertices.length; j++){
                var v = shape.vertices[j];
                vec2.rotate(vrot, v, shape.angle);
                verts.push([(vrot[0]+offset[0]), (vrot[1]+offset[1])]);
            }

            g.lineStyle(lineWidth, this.lineColor, 1);
            g.beginFill(color, alpha);
            for(var i=0; i!==verts.length; i++){
                var v = verts[i],
                    x = v[0],
                    y = v[1];
                if(i===0){
                    g.moveTo(x,y);
                } else {
                    g.lineTo(x,y);
                }
            }
            g.endFill();

            if(verts.length>2){
                g.moveTo(verts[verts.length-1][0],verts[verts.length-1][1]);
                g.lineTo(verts[0][0],verts[0][1]);
            }
        };

    WebGLRenderer.prototype.drawPath = function(g, path, lineWidth, lineColor, fillColor, fillAlpha){
        g.lineStyle(lineWidth, lineColor, 1);

        if(fillAlpha !== 0){
            g.beginFill(fillColor, fillAlpha);
        }

        var lastx = null, lasty = null;
        for(var i=0; i<path.length+1; i++){
            var v = path[i % path.length],
                x = v[0],
                y = v[1];
            if(x !== lastx || y !== lasty){
                if (i === 0) {
                    g.moveTo(x,y);
                } else {
                    // Check if the lines are parallel
                    var p1x = lastx,
                        p1y = lasty,
                        p2x = x,
                        p2y = y,
                        p3x = path[(i+1)%path.length][0],
                        p3y = path[(i+1)%path.length][1];
                    var area = ((p2x - p1x)*(p3y - p1y))-((p3x - p1x)*(p2y - p1y));
                    if(area !== 0){
                        g.lineTo(x,y);
                    }
                }
                lastx = x;
                lasty = y;
            }
        }

        if(fillAlpha !== 0){
            g.endFill();
        }

        // Close the path
        if(path.length>2 && fillAlpha !== 0){
            g.moveTo(path[path.length-1][0], path[path.length-1][1]);
            g.lineTo(path[0][0],path[0][1]);
        }
    };

    WebGLRenderer.prototype.updateSpriteTransform = function(sprite,body){
        if(this.useInterpolatedPositions && !this.paused){
            sprite.position.set(body.interpolatedPosition[0], body.interpolatedPosition[1]);
            sprite.rotation = body.interpolatedAngle;
        } else {
            sprite.position.set(body.position[0], body.position[1]);
            sprite.rotation = body.angle;
        }
    };

    var X = vec2.fromValues(1,0),
        distVec = vec2.fromValues(0,0),
        worldAnchorA = vec2.fromValues(0,0),
        worldAnchorB = vec2.fromValues(0,0);
    WebGLRenderer.prototype.render = function(deltaTime){
        var stage = this.stage;
        var springSprites = this.springSprites;

        // Damp position
        this.currentStageX = smoothDamp(this.currentStageX, this.renderer.width / 2 - this.zoom * this.cameraPosition[0], this._cameraStoreX, deltaTime, this.smoothTime, this.maxSmoothVelocity);
        this.currentStageY = smoothDamp(this.currentStageY, this.renderer.height / 2 + this.zoom * this.cameraPosition[1], this._cameraStoreY, deltaTime, this.smoothTime, this.maxSmoothVelocity);
        this.stage.position.set(
            this.currentStageX,
            this.currentStageY
        );

        // Damp zoom
        this.currentZoom = smoothDamp(this.currentZoom, this.zoom, this._zoomStore, deltaTime, this.smoothTime, this.maxSmoothVelocity);
        this.stage.scale.set(this.currentZoom, -this.currentZoom);

        this.stage.updateTransform();

        // Update body transforms
        for(var i=0; i!==this.bodies.length; i++){
            this.updateSpriteTransform(this.sprites[i],this.bodies[i]);
        }

        // Update graphics if the body changed sleepState or island
        for(var i=0; i!==this.bodies.length; i++){
            var body = this.bodies[i];
            var isSleeping = (body.sleepState===p2.Body.SLEEPING);
            var sprite = this.sprites[i];
            var islandColor = this.getIslandColor(body);
            if(sprite.drawnSleeping !== isSleeping || sprite.drawnColor !== islandColor){
                sprite.clear();
                this.drawRenderable(body, sprite, islandColor, sprite.drawnLineColor);
            }
        }

        // Update spring transforms
        for(var i=0; i!==this.springs.length; i++){
            var s = this.springs[i],
                sprite = springSprites[i],
                bA = s.bodyA,
                bB = s.bodyB;

            if(this.useInterpolatedPositions && !this.paused){
                vec2.toGlobalFrame(worldAnchorA, s.localAnchorA, bA.interpolatedPosition, bA.interpolatedAngle);
                vec2.toGlobalFrame(worldAnchorB, s.localAnchorB, bB.interpolatedPosition, bB.interpolatedAngle);
            } else {
                s.getWorldAnchorA(worldAnchorA);
                s.getWorldAnchorB(worldAnchorB);
            }

            sprite.scale.y = 1;
            if(worldAnchorA[1] < worldAnchorB[1]){
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
            sprite.position.set(
                ( sxA + sxB ) / 2,
                ( syA + syB ) / 2
            );

            // Compute distance vector between anchors, in screen coords
            vec2.set(distVec, sxA - sxB, syA - syB);

            // Compute angle
            sprite.rotation = Math.acos( vec2.dot(X, distVec) / vec2.length(distVec) );

            // And scale
            sprite.scale.x = vec2.length(distVec) / s.restLength;
        }

        // Clear contacts
        if(this.drawContacts){
            this.contactGraphics.clear();

            // Keep it on top
            stage.removeChild(this.contactGraphics);
            stage.addChild(this.contactGraphics);

            var g = this.contactGraphics;
            g.lineStyle(this.lineWidth, 0x000000, 1);
            for(var i=0; i!==this.world.narrowphase.contactEquations.length; i++){
                var eq = this.world.narrowphase.contactEquations[i],
                    bi = eq.bodyA,
                    bj = eq.bodyB,
                    ri = eq.contactPointA,
                    rj = eq.contactPointB,
                    xi = bi.position[0],
                    yi = bi.position[1],
                    xj = bj.position[0],
                    yj = bj.position[1];

                g.moveTo(xi,yi);
                g.lineTo(xi+ri[0], yi+ri[1]);

                g.moveTo(xj,yj);
                g.lineTo(xj+rj[0], yj+rj[1]);

            }
            this.contactGraphics.cleared = false;
        } else if(!this.contactGraphics.cleared){
            this.contactGraphics.clear();
            this.contactGraphics.cleared = true;
        }

        // Draw AABBs
        if(this.drawAABBs){
            this.aabbGraphics.clear();
            stage.removeChild(this.aabbGraphics);
            stage.addChild(this.aabbGraphics);
            var g = this.aabbGraphics;
            g.lineStyle(this.lineWidth,0x000000,1);

            for(var i=0; i!==this.world.bodies.length; i++){
                var aabb = this.world.bodies[i].getAABB();
                g.drawRect(aabb.lowerBound[0], aabb.lowerBound[1], aabb.upperBound[0] - aabb.lowerBound[0], aabb.upperBound[1] - aabb.lowerBound[1]);
            }
            this.aabbGraphics.cleared = false;
        } else if(!this.aabbGraphics.cleared){
            this.aabbGraphics.clear();
            this.aabbGraphics.cleared = true;
        }

        // Draw pick line
        if(this.mouseConstraint){
            var g = this.pickGraphics;
            g.clear();
            stage.removeChild(g);
            stage.addChild(g);
            g.lineStyle(this.lineWidth,0x000000,1);
            var c = this.mouseConstraint;
            var worldPivotB = vec2.create();
            c.bodyB.toWorldFrame(worldPivotB, c.pivotB);
            g.moveTo(c.pivotA[0], c.pivotA[1]);
            g.lineTo(worldPivotB[0], worldPivotB[1]);
            g.cleared = false;
        } else if(!this.pickGraphics.cleared){
            this.pickGraphics.clear();
            this.pickGraphics.cleared = true;
        }

        if(this.followBody){
            this.setCameraCenter(this.followBody.interpolatedPosition);
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
    WebGLRenderer.randomColor = function(){
        var mix = [255,255,255];
        var red =   Math.floor(Math.random()*256);
        var green = Math.floor(Math.random()*256);
        var blue =  Math.floor(Math.random()*256);

        // mix the color
        red =   Math.floor((red +   3*mix[0]) / 4);
        green = Math.floor((green + 3*mix[1]) / 4);
        blue =  Math.floor((blue +  3*mix[2]) / 4);

        return rgbToHex(red,green,blue);
    };

    WebGLRenderer.prototype.drawRenderable = function(obj, graphics, color, lineColor){
        var lw = this.lineWidth;

        graphics.drawnSleeping = false;
        graphics.drawnColor = color;
        graphics.drawnLineColor = lineColor;
        if(obj instanceof p2.Body && obj.shapes.length){

            var isSleeping = (obj.sleepState === p2.Body.SLEEPING);
            var alpha = isSleeping ? this.sleepOpacity : 1;

            graphics.drawnSleeping = isSleeping;

            if(this.bodyPolygonPaths[obj.id] && !this.debugPolygons){
                // Special case: bodies created using Body#fromPolygon
                this.drawPath(graphics, this.bodyPolygonPaths[obj.id], lw, lineColor, color, alpha);
            } else {
                // Draw all shapes
                for(var i=0; i<obj.shapes.length; i++){
                    var child = obj.shapes[i];

                    var drawFunction = this.shapeDrawFunctions[child.type];
                    if(drawFunction){
                        drawFunction.call(this, graphics, child, color, alpha, lw);
                    }
                }
            }

        } else if(obj instanceof p2.Spring){
            var restLengthPixels = obj.restLength;
            this.drawSpring(graphics, restLengthPixels, lineColor, lw);
        }
    };

    WebGLRenderer.prototype.getIslandColor = function(body){
        var islandColors = this.islandColors;
        var color;
        if(body.islandId === -1){
            color = this.islandStaticColor; // Gray for static objects
        } else if(islandColors[body.islandId]){
            color = islandColors[body.islandId];
        } else {
            color = islandColors[body.islandId] = parseInt(WebGLRenderer.randomColor(),16);
        }
        return color;
    };

    WebGLRenderer.prototype.addRenderable = function(obj){

        var lineColor = this.lineColor;

        var sprite = new PIXI.Graphics();
        if(obj instanceof p2.Body && obj.shapes.length){

            var color = this.getIslandColor(obj);
            this.drawRenderable(obj, sprite, color, lineColor);
            this.sprites.push(sprite);
            this.stage.addChild(sprite);

        } else if(obj instanceof p2.Spring){

            this.drawRenderable(obj, sprite, lineColor, lineColor);
            this.springSprites.push(sprite);
            this.stage.addChild(sprite);

        }
    };

    WebGLRenderer.prototype.removeRenderable = function(obj){
        if(obj instanceof p2.Body){
            var i = this.bodies.indexOf(obj);
            if(i!==-1){
                this.stage.removeChild(this.sprites[i]);
                this.sprites.splice(i,1);
            }
        } else if(obj instanceof p2.Spring){
            var i = this.springs.indexOf(obj);
            if(i!==-1){
                this.stage.removeChild(this.springSprites[i]);
                this.springSprites.splice(i,1);
            }
        }
    };

    WebGLRenderer.prototype.resize = function(w,h){
        this.renderer.resize(w, h);
    };
}