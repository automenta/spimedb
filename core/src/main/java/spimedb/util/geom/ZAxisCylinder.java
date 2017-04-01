/*
 *   __               .__       .__  ._____.           
 * _/  |_  _______  __|__| ____ |  | |__\_ |__   ______
 * \   __\/  _ \  \/  /  |/ ___\|  | |  || __ \ /  ___/
 *  |  | (  <_> >    <|  \  \___|  |_|  || \_\ \\___ \ 
 *  |__|  \____/__/\_ \__|\___  >____/__||___  /____  >
 *                   \/       \/             \/     \/ 
 *
 * Copyright (c) 2006-2011 Karsten Schmidt
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 */

package spimedb.util.geom;

public class ZAxisCylinder extends AxisAlignedCylinder {

    public ZAxisCylinder(roVec3D pos, float radius, float length) {
        super(pos, radius, length);
    }

    public boolean containsPoint(XYZ p) {
        if (Math.abs(p.z() - pos.z) < length * 0.5) {
            float dx = p.x() - pos.x;
            float dy = p.y() - pos.y;
            if (Math.abs(dx * dx + dy * dy) < radiusSquared) {
                return true;
            }
        }
        return false;
    }

    public Vec3D.Axis getMajorAxis() {
        return Vec3D.Axis.Z;
    }
}
