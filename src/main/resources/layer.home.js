class HomeLayer extends Layer {
    constructor() {
        super('Home');
        this.label = $('<div>');
    }

    load() {
        try {
            this.pos = JSON.parse(localStorage.home);
            //this.label.text(JSON.stringify(this.pos));
        } catch (e) {
            this.pos = undefined;
            delete localStorage.home; //corrupt?
        }
    }

    save() {
        localStorage.home = JSON.stringify(this.pos);
        this.load();
    }

    layerIcon(focus) {
        if (!this.pos) {
            this.load();
            this.go(focus);
        }

        const d = $('<div>');
        d.append('<i class="buttonlike fa fa-home" style="font-size: 200%; float: left;"/>');
        d.append(this.label);
        d.append($('<div>').text('Go').addClass('buttonlike').click(() => {
            this.go(focus);
        }));
        d.append($('<div>').text('Set').addClass('buttonlike').click(() => {
            const p = focus.view.focus.view.pos();
            this.pos = _.clone(p);
            this.save();
            this.go(focus);
        }));
        //load(focus.home);
        return d;
    }

    go(focus) {
        setTimeout(()=>{
            const pos = this.pos;
            console.log('go', pos);
            if (pos) {
                focus.position(pos.latitude, pos.longitude, pos.altitude);
                focus.view.redraw();
            }
        });
    }
}
