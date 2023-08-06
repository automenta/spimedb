class WorldWindView extends View {

    constructor() {
        super();

        {
            //https://html.spec.whatwg.org/multipage/canvas.html#concept-canvas-will-read-frequently
            if (WorldWind.SurfaceShapeTile.canvas)
                throw "instantiation problem";
            WorldWind.SurfaceShapeTile.canvas = document.createElement("canvas");
            WorldWind.SurfaceShapeTile.ctx2D = WorldWind.SurfaceShapeTile.canvas.getContext("2d", {
                // desynchronized: true,
                willReadFrequently: true
            });
            WorldWind.SurfaceShape.DEFAULT_NUM_EDGE_INTERVALS = 32; //default=128
        }
    }

    start(target, focus) {
        super.start(target, focus);
        //<canvas id="map" className="content">HTML5 canvas unsupported</canvas>
        const elementID = "_map";
        this.canvas = $('<canvas id="' + elementID + '" class="content">HTML5 canvas unsupported</canvas>');
        this.target.append(this.canvas);

        //console.log(WorldWind.configuration);
        //WorldWind.configuration.gpuCacheSize = 1e9; // 1gb
        WorldWind.Logger.setLoggingLevel(WorldWind.Logger.LEVEL_WARNING);

        const w = new WorldWind.WorldWindow(elementID);
        this.w = w;

        //w.pixelScale = 0.5; //lo-res
        //w.deepPicking = true;
        //w.camera.fieldOfView = 40;
        //console.error(w);

        let anim = null;

        // The common pick-handling function.
        // Listen for mouse moves and highlight the placemarks that the cursor rolls over.
        // Listen for taps on mobile devices and highlight the placemarks that the user taps.
        let finger = o => {
            if (o.buttons !== 1)
                return;

            setTimeout(()=>{
                let picked;
                try {
                    picked = w.pick(w.canvasCoordinates(o.clientX, o.clientY));
                } catch (e) {
                    console.log(e);
                    return;
                }

                const picks = picked.objects;
                //console.log('pick', picks);

                if (picks.length > 0) {
                    const x = picks[picks.length - 1];
                    //_.forEach(picks, x => {
                    if (x.isTerrain) {
                        //toastr.info("terrain " + x.position);
                    } else {
                        // if (anim === null) {
                        //
                        //     const tgt = new WorldWind.Position().copy(x.userObject.referencePosition);
                        //     tgt.altitude += 100;
                        //
                        //     anim = new WorldWind.GoToAnimator(w);
                        //     anim.travelTime = 1000;
                        //     anim.goTo(tgt, () => {
                        //         anim = null;
                        //     });
                        // }

                        if (x.userObject) {
                            if (x.userObject.nobject) {
                                console.log('pick', x.userObject.nobject);
                            }
                        }

                        // const concept = x.userObject.userProperties.concept || x.userObject.displayName;
                        // toastr.info(JSON.stringify(concept), {
                        //     closeButton: true
                        // });
                    }

                }
            }, 0);
            // _.forEach(picks, p => {
            //     const o = p.userObject;
            //     o.highlighted = true;
            //     touched.push(o);
            // })
            //view.redraw(); // redraw to make the highlighting changes take effect on the screen

        };
        //finger = _.throttle(finger, 50);

        //var tapRecognizer = new WorldWind.TapRecognizer(view, finger);
        //view.addEventListener("mousemove", finger);
        w.addEventListener("pointerdown", finger);

        let lastCam = undefined;
        window.setInterval(() => {
            const cam = this.w.camera;
            if (!cam.equals(lastCam)) {
                focus.event.emit('view_change', cam);
                //console.log(cam);
                lastCam = cam.clone();
            }
        }, 50);

        return this;
    }

    stop() {
        this.canvas.remove();
        this.w = null;
        super.stop();
    }

    redraw() {
        this.w.redraw();
    }

    pos() {
        return this.w.camera.position;
    }
    addLayer(l) {
        this.w.addLayer(l);
    }
    removeLayer(l) {
        this.w.removeLayer(l);
    }

}
