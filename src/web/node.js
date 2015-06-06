/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

function replaceWidget(key, getWidget) {
    return {
        apply: function(n) {
            var d = n.data();
            var l = d[key];
            if (l) {                                
                d.widget = getWidget(l);                                
                n.cy().removeNodeWidget(n[0]);
            }
        }
    };    
}

function UrlToIFrame() {
    return replaceWidget('url', function(u) {
        var h = '<iframe style="width:100%;height:100%" src="' + u + '"></iframe>';
        
        return {
            html: h,
            pixelScale: 500.0,
            scale: 0.9
        };
    });    
}

function ListToText() {

    return replaceWidget('list', function(l) {
        var h = '<textarea style="width:100%;height:100%" editable="false">';
        for (var i = 0; i < l.length; i++)
            h += l[i] + '\n'; //<br/>';
        h+= '</textarea>';
        
        return {
            html: h,
            pixelScale: 300.0,                    
            scale: 0.9
        };
    });
}