"use strict";

function NSlider(opt) {

	opt = opt || { };

	opt = {
		onChange: opt.onChange,
		onRelease: opt.onRelease,
		onPress: opt.onPress


	};

	/** value */
	var p;

	var slider = $('<div>&nbsp;</div>').addClass('zoomSlider');

	var bar = $('<span>&nbsp;</span>').addClass('bar').appendTo(slider);


	bar.css({
		backgroundColor: 'blue',
		height: '100%',
		position: 'relative',
		left: 0,
		top: 0

	});

	var mousedown = false;

	function update(e) {
		var s = slider;
		var px = s.offset().left;
		var x = e.clientX - px;

		p = (parseFloat(x) / parseFloat(s.width()));

		var cr = parseInt(255.0 * p);
		var cg = parseInt(25.0);
		var cb = parseInt(255.0 * (1.0 - p));

		setTimeout(function() {
			var cc = 'rgb(' + cr + ',' + cg + ',' + cb + ')';
			bar.css({
				width: x + 'px',
				backgroundColor: cc
			});
		},0);

		if (opt.onChange)
			opt.onChange(p, slider);

		return p;
	}

	slider.mouseup(function(e) {
		var p = update(e);
		mousedown = false;

		if (opt.onRelease)
			opt.onRelease(p, slider);

		return false;
	});
	slider.mousedown(function(e) {
		if (e.which == 1) {

			mousedown = true;

			if (opt.onPress)
				opt.onPress(p, slider);
		}
		return false;
	});

	slider.mousemove(function(e) {
		if (e.which == 0) mousedown = false;

		if (mousedown) {
			update(e);
		}
	});

	/*slider.mousewheel(function(evt){
	 var direction = evt.deltaY;
	 if (direction < 0) {
	 scaleNode(1.2);
	 }
	 else {
	 scaleNode(1.0/1.2);
	 }
	 return false;
	 });*/

	//slider.mouseleave(function(e) { mousedown = false; });



	return slider;
}
