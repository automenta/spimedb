"use strict";
//import $ from 'jquery';
import Spime from './spime.js';
//import dat from 'dat.gui';
//console.error(dat);


const p2 = Spime.p2;

Spime.p2webgl(p2, 30 /* fps */); //awkward, fix it


export default function _spacegraphNew(targetEle) {

    const world = new p2.World({
        gravity: [0, 0]
    });


    const sg = new p2.WebGLRenderer(function () {

        this.setWorld(world);


        world.on("addBody", function (evt) {
            evt.body.setDensity(1);
        });

        // Change the current engine torque with the left/right keys
        this.on("keydown", function (evt) {
            // t = 5;
            // switch (evt.keyCode) {
            //     case 39: // right
            //         torque = -t;
            //         break;
            //     case 37: // left
            //         torque = t;
            //         break;
            // }
        });
        this.on("keyup", function () {
            //torque = 0;
        });
    });
    targetEle.prepend(sg.element);

    sg.p2 = p2;

    return sg;
};

window.spacegraphNew = (targetEle, withSpaceGraphFunc) => {
    withSpaceGraphFunc( _spacegraphNew(targetEle) );
};


//
// var stiffness = 100,
//     damping = 5,
//     restLength = 0.5;


// // Set high friction so the wheels don't slip
// world.defaultContactMaterial.friction = 100;
// // Create ground
// var planeShape = new p2.Plane();
// var plane = new p2.Body();
// plane.addShape(planeShape);
// world.addBody(plane);
// Create chassis
//var chassisBody = new p2.Body({mass: 1, position: [0, 1]}),
//     chassisShape = new p2.Box({width: 1, height: 0.5});
// chassisBody.addShape(chassisShape);
// world.addBody(chassisBody);
// // Create wheels
// var wheelBody1 = new p2.Body({mass: 1, position: [chassisBody.position[0] - 0.5, 0.7]}),
//     wheelBody2 = new p2.Body({mass: 1, position: [chassisBody.position[0] + 0.5, 0.7]}),
//     wheelShape1 = new p2.Circle({radius: 0.3}),
//     wheelShape2 = new p2.Circle({radius: 0.3});
// wheelBody1.addShape(wheelShape1);
// wheelBody2.addShape(wheelShape2);
// world.addBody(wheelBody1);
// world.addBody(wheelBody2);
// // Disable collisions between chassis and wheels
// var WHEELS = 1, // Define bits for each shape type
//     CHASSIS = 2,
//     GROUND = 4,
//     OTHER = 8;
// wheelShape1.collisionGroup = wheelShape2.collisionGroup = WHEELS; // Assign groups
// chassisShape.collisionGroup = CHASSIS;
// planeShape.collisionGroup = GROUND;
// wheelShape1.collisionMask = wheelShape2.collisionMask = GROUND | OTHER;             // Wheels can only collide with ground
// chassisShape.collisionMask = GROUND | OTHER;             // Chassis can only collide with ground
// planeShape.collisionMask = WHEELS | CHASSIS | OTHER;   // Ground can collide with wheels and chassis
// // Constrain wheels to chassis
// var c1 = new p2.PrismaticConstraint(chassisBody, wheelBody1, {
//     localAnchorA: [-0.5, -0.3],
//     localAnchorB: [0, 0],
//     localAxisA: [0, 1],
//     disableRotationalLock: true,
// });
// var c2 = new p2.PrismaticConstraint(chassisBody, wheelBody2, {
//     localAnchorA: [0.5, -0.3],
//     localAnchorB: [0, 0],
//     localAxisA: [0, 1],
//     disableRotationalLock: true,
// });
// c1.setLimits(-0.4, 0.2);
// c2.setLimits(-0.4, 0.2);
// world.addConstraint(c1);
// world.addConstraint(c2);
// // Add springs for the suspension
// // Left spring
// world.addSpring(new p2.LinearSpring(chassisBody, wheelBody1, {
//     restLength: restLength,
//     stiffness: stiffness,
//     damping: damping,
//     localAnchorA: [-0.5, 0],
//     localAnchorB: [0, 0],
// }));
// // Right spring
// world.addSpring(new p2.LinearSpring(chassisBody, wheelBody2, {
//     restLength: restLength,
//     stiffness: stiffness,
//     damping: damping,
//     localAnchorA: [0.5, 0],
//     localAnchorB: [0, 0],
// }));
// this.newShapeCollisionGroup = OTHER;
// this.newShapeCollisionMask = GROUND | WHEELS | CHASSIS | OTHER;
// // Apply current engine torque after each step
// var torque = 0;
// world.on("postStep", function (evt) {
//     var max = 100;
//     if (wheelBody1.angularVelocity * torque < max) wheelBody1.angularForce += torque;
//     if (wheelBody2.angularVelocity * torque < max) wheelBody2.angularForce += torque;
// });