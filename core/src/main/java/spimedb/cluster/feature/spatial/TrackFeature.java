/**
 * Copyright (c) 2013 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package spimedb.cluster.feature.spatial;

import spimedb.cluster.feature.Feature;
import spimedb.util.geom.geodesic.Track;

/***
 * A TrackFeature represents a sequence of geo-spatial latitude and longitude coordinates
 * 
 * @author slangevin
 *
 */
public class TrackFeature extends Feature {
    private static final long serialVersionUID = -7923265691042687996L;



    private Track track;
    
    @Override
    public String toString() {
        return (this.getName() + " : " + track.toString());
    }
    
    public TrackFeature() {
        super();
    }
    
    public TrackFeature(String name) {
        super(name);
    }

    public void setValue(Track track) {
        this.track = track;
    }
    
    public Track getValue() {
        return track;
    }
}
