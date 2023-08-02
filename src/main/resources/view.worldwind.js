class WorldWindView extends View {

    constructor() {
        super();
    }

    start(target, focus) {
        super.start(target, focus);
        //<canvas id="map" className="content">HTML5 canvas unsupported</canvas>
        const elementID = "_map";
        this.canvas = $('<canvas id="' + elementID + '" class="content">HTML5 canvas unsupported</canvas>');
        this.target.append(this.canvas);

        //console.log(WorldWind.configuration);
        WorldWind.configuration.gpuCacheSize = 1e9; // 1gb
        WorldWind.Logger.setLoggingLevel(WorldWind.Logger.LEVEL_WARNING);

        const view = new WorldWind.WorldWindow(elementID);
        this.w = view;

        //w.deepPicking = true;
        //w.camera.fieldOfView = 40;

        let anim = null;

        // The common pick-handling function.
        // Listen for mouse moves and highlight the placemarks that the cursor rolls over.
        // Listen for taps on mobile devices and highlight the placemarks that the user taps.
        let finger = o => {
            if (o.buttons !== 1)
                return;

            let picked;
            try {
                picked = view.pick(view.canvasCoordinates(o.clientX, o.clientY));
            } catch (e) {
                console.log(e);
                return;
            }

            const picks = picked.objects;
            console.log('pick', picks);

            if (picks.length > 0) {
                const x = picks[picks.length - 1];
                //_.forEach(picks, x => {
                if (x.isTerrain) {
                    //toastr.info("terrain " + x.position);
                } else {
                    if (anim === null) {

                        const tgt = new WorldWind.Position().copy(x.userObject.referencePosition);
                        tgt.altitude += 100;

                        anim = new WorldWind.GoToAnimator(view);
                        anim.travelTime = 1000;
                        anim.goTo(tgt, () => {
                            anim = null;
                        });
                    }

                    // const concept = x.userObject.userProperties.concept || x.userObject.displayName;
                    // toastr.info(JSON.stringify(concept), {
                    //     closeButton: true
                    // });
                }
            }
            // _.forEach(picks, p => {
            //     const o = p.userObject;
            //     o.highlighted = true;
            //     touched.push(o);
            // })
            //view.redraw(); // redraw to make the highlighting changes take effect on the screen

        };
        finger = _.throttle(finger, 100);

        //var tapRecognizer = new WorldWind.TapRecognizer(view, finger);
        //view.addEventListener("mousemove", finger);
        view.addEventListener("pointerdown", finger);

        let lastCam = undefined;
        window.setInterval(() => {
            const cam = this.w.camera.clone();
            if (!cam.equals(lastCam)) {
                focus.event.emit('view_change', cam);
                //console.log(cam);
            }
            lastCam = cam;
        }, 100);

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
