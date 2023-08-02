class View {
    constructor() {

    }

    start(target, focus) {

        if (focus.view)
            throw "TODO support multiple views";

        focus.view = this;

        this.focus = focus;
        this.target = target;
        return this;
    }

    pos() {

    }

    redraw() {

    }

    stop() {
        this.focus = null;
        //TODO this.target.removeChildren()
        this.target = null;
    }

    addLayer(l) {

    }
    removeLayer(l) {

    }
}