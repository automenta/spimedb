class Layer {
    constructor(name) {
        this.name = name;
        this.enabled = true;
    }

    start(focus) {
        const e = this.icon = $('<div>');
        e.attr('class', 'cell').append(this.layerIcon(focus));
        $('#menu').append(e);
        this.enabled = this.enabled; //force update
    }

    stop(focus) {
        this.icon.remove();
        this.icon.html('');
    }

    update(focus) {
        //
    }

    set enabled(e) {
        console.log(this, e);
        //if (this._enabled !== e) {
            this._enabled = e;
            const ele = this.icon;
            if (ele) {
                if (e) ele.addClass   ('cell_enabled');
                else   ele.removeClass('cell_enabled');
            }
        //}
    }

    /** called when position is updated */
    position(pos) {

    }

    get enabled() {
        return this._enabled;
    }

    enable() {
        this.enabled = true;
        return this;
    }

    disable() {
        this.enabled = false;
        return this;
    }

    /* produce an element for display in list */
    layerIcon(focus) {
        return $('<button>').text(this.name);
    }
}