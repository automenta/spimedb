class GeoLayer extends Layer {

    constructor(name, l) {
        super(name);
        this.layer = l;
    }

    //TODO set opacity ...

    opacity(o) {
        this.layer.opacity = o;
        return this;
    }

    start(focus) {
        super.start(focus);
        focus.view.addLayer(this.layer);
    }

    stop(focus) {
        super.stop(focus);
        focus.view.removeLayer(this.layer);
    }

    enable() {
        super.enable();
        this.layer.enabled = true;
        return this;
    }

    disable() {
        super.disable();
        this.layer.enabled = false;
        return this;
    }

    layerIcon(focus) {
        const layer = this.layer;

        //const toggleLabel = () => (this.enabled ? "+" : "-") + this.name;

        const label = $('<div>')
            .addClass('label')
            .addClass('buttonlike')
            .attr('style', 'text-align: left;')
            //.text(this.name); //toggleLabel());

        const enablement = () => {
            //label.text(toggleLabel());
            label.text(this.name);
        };

        enablement(); //initialize

        label.click(() => {
            enablement();
            setTimeout(()=>{
                if (layer.enabled) this.disable(); else this.enable();
                focus.view.redraw();
            });
        });


        //https://github.com/dataarts/dat.gui/blob/master/API.md
        // const gui = new dat.GUI({
        //     name: 'xyz', autoPlace: false, hideable: false,
        //     width: 'auto'
        // });
        // const person = {name: 'wtf', age: 45, alive: false};
        // gui.add(person, 'name'); // string control
        //
        // gui.add(person, 'age', 0, 100); // Add a number controller slider.
        //
        // gui.add(person, 'alive');
        // //const folder1 = gui.addFolder('Flow Field');
        // const palette = {
        //     color1: '#FF0000', // CSS string
        //     color2: [0, 128, 255], // RGB array
        //     color3: [0, 128, 255, 0.3], // RGB with alpha
        //     color4: {h: 350, s: 0.9, v: 0.3} // Hue, saturation, value
        // };
        // gui.addColor(palette, 'color1');
        // gui.addColor(palette, 'color2');
        // gui.addColor(palette, 'color3');
        // gui.addColor(palette, 'color4');
        // const controls = $('<div>').addClass('controls').hide().append(
        //     gui.domElement
        // );
        // const expander = $('<div>').addClass('buttonlike')
        //     .text('⇅').attr('style', 'text-align: right').click(() => {
        //         controls.toggle();
        //     });

        return $('<div>').append(label
            //, expander, controls
        );
    }
}
