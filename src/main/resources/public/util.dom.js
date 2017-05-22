"use strict";

function e(eleID, cssclass) {
    var x = document.createElement(eleID);
    if (cssclass) {
        x.setAttribute('class', cssclass);
    }
    return x;
}
function E(eleID, cssclass) {
    return $(e(eleID, cssclass));
}

function D(cssclass) {
    return E('div', cssclass);
}
