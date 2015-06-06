/* 
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
Include:
<script src="lib/sockjs/sockjs-0.3.4.min.js"></script>
<script src='lib/sockjs/vertxbus.min.js'></script>
*/

var bus = new vertx.EventBus('/eventbus');
bus.on = bus.registerHandler;

$(document).ready(function() {

    bus.onopen = function() {

        //  
        //  bus.on('public', function(message) {
        //
        //    try {
        //        console.dir(JSON.parse(message));
        //    }
        //    catch (e) {
        //        console.error('Unable to parse: ' + message);
        //    }
        //
        //  });
          console.log('Websocket Event Bus start');
          $N.trigger('bus.start');

    };
    
});
