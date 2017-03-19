(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
/* MIT License

rule-reactor: A light weight, fast, expressive forward chaining business rule engine leveraging JavaScript internals and Functions as objects rather than Rete.

Copyright (c) 2016 Simon Y. Blackwell

*/

var uuid = require("uuid");
//var hamsters = require("webhamsters/src/hamsters");
//var Parallel = require("paralleljs");

(function() {
	"use strict";
	
	function intersector(objects) {
		return function intersection() {
			var min = Infinity, // length of shortest array argument
				shrtst = 0, // index of shortest array argument
				set = (objects ? new Set() : {}),
				rslt = [], // result
				mxj = arguments.length-1;
			for(var j=0;j<=mxj;j++) { // find index of shortest array argument
				var l = arguments[j].length;
				if(l<min) {
					shrtst = j;
					min = l;
				}
			}
			var shrt = arguments[shrtst],
				mxi = shrt.length;
			for(var i=0;i<mxi;i++) { // initialize set of possible values from shortest array
				if(objects) { set.add(shrt[i]); } else { set[shrt[i]]=1; }
			}
			for(var j=0;j<=mxj;j++) { // loop through all array arguments
				var	array = arguments[j],
					mxk = array.length;
				for(var k=0;k<mxk;k++) { // loop through all values
					var item = array[k];
					if((objects && set.has(item)) || set[item]) { // if value is possible
						if(j===mxj) { // and all arrays have it (or we would not be at this point)
							rslt.push(item); // add to results
						}
					}
				}
			}
			return rslt;
		};
	}
	var intersection = intersector(false);
	
//		portions from http://phrogz.net/lazy-cartesian-product
	function CXProduct(collections){
		var me = this;
		me.collections = (collections ? collections : []);
	}
	CXProduct.prototype.length = function() {
		var me = this;
		if(me.collections.length===0) {
			return 0;
		}
		var size = 1;
		this.collections.forEach(function(collection) { size *= collection.length; });
		if(typeof(me.start)==="number") {
			if(typeof(me.end)==="number") {
				return me.end - me.start;
			}
			return size - me.start;
		}
		return size; 
	};
	CXProduct.prototype.length.size = CXProduct.prototype.length;
	CXProduct.prototype.every = function(callback,pattern) {
		function dive(cxproduct,d,counter,collections,lens,p,callback,pattern){
			var a=collections[d], max=collections.length-1,len=lens[d];
			if (d===max) {
				for (var i=0;i<len;++i) { 
					p[d]=a[i]; 
					if(!callback(p.slice(0),counter.count)) {
						return false;
					}
					counter.count++;
				}
			} else {
				for (var j=0;j<len;++j) {
					p[d]=a[j];
					dive(cxproduct,d+1,counter,collections,lens,p,callback,pattern);
				}
			}
			p.pop();
		}
		if(typeof(start)==="number") {
			return this.some2(callback,pattern);
		}
		var me = this, p=[],lens=[];
		for (var i=me.collections.length;i--;) { lens[i]=me.collections[i].length; }
		if(dive(me,0,{count:0},me.collections,lens,p,callback,pattern)!==false) {
			return true;
		}
	};
	CXProduct.prototype.get = function(index,pattern){
		function get(n,collections,dm,c) {
			for (var i=collections.length;i--;) { c[i]=collections[i][(n/dm[i][0]<<0)%dm[i][1]]; }
		}
		var me = this, dm = [], c = [];
		for (var f=1,l,i=me.collections.length;i--;f*=l) { dm[i]=[f,l=me.collections[i].length];  }
		if(index>=me.length()) {
			return; // undefined
		}
		get(index,me.collections,dm,c);
		if(!pattern || pattern.every(function(value,i) {
			return typeof(value)==="undefined" || (typeof(value)==="function" ? value.call(c,c[i],i) : false) || c[i]===value;
		})) {
			return c.slice(0);
		}
	}
	CXProduct.prototype.some = function(callback,pattern) {
		function dive(cxproduct,d,counter,collections,lens,p,callback,pattern){
			var a=collections[d], max=collections.length-1,len=lens[d];
			if (d===max) {
				for (var i=0;i<len;++i) { 
					p[d]=a[i]; 
					if(callback(p.slice(0),counter.count)) {
						return true;
					}
					counter.count++;
				}
			} else {
				for (var j=0;j<len;++j) {
					p[d]=a[j];
					dive(cxproduct,d+1,counter,collections,lens,p,callback,pattern);
				}
			}
			p.pop();
		}
		if(typeof(start)==="number") {
			return this.some2(callback,pattern);
		}
		var me = this, p=[],lens=[];
		for (var i=me.collections.length;i--;) { lens[i]=me.collections[i].length; }
		return dive(me,0,{count:0},me.collections,lens,p,callback,pattern);
	}
	CXProduct.prototype.some2 = function(callback,pattern) {
		var me = this, i = 0, value, max = me.length();
		do {
			value = me.get(i);
			if(typeof(value)!=="undefined" && (!callback || callback(value)) && (!pattern || me.testpattern(pattern,value))) {
				return true;
			}
			i++;
		} while(i<max); 
		return false;
	}
	CXProduct.prototype.verify = function(i,row) {
		var me = this;
		var match = me.get(i);
		return match && match.every(function(element,i) { return element===row[i]; });
	}
	CXProduct.prototype.forEach1 = function(callback) {
		function dive(cxproduct,d,counter,collections,lens,p,callback){
			var a=collections[d], max=collections.length-1,len=lens[d];
			if (d===max) {
				for (var i=0;i<len;++i) { 
					p[d]=a[i]; 
					callback(p.slice(0),counter.count); 
					counter.count++;
				}
			} else {
				for (var j=0;j<len;++j) {
					p[d]=a[j];
					dive(cxproduct,d+1,counter,collections,lens,p,callback);
				}
			}
			p.pop();
		}
		if(typeof(start)==="number") {
			this.forEach2(callback);
			return;
		}
		var me = this, p=[],lens=[];
		for (var i=me.collections.length;i--;) { lens[i]=me.collections[i].length; }
		dive(me,0,{count:0},me.collections,lens,p,callback);
	}
	CXProduct.prototype.forEach2 = function(callback) {
		var me = this, i = (typeof(me.start)==="number" ? me.start : 0), max = (typeof(me.end)==="number" ? me.end : me.length());
		while(i<max) {
			var value = me.get(i);
			if(typeof(value)!=="undefined") {
				callback(value);
			}
			i++;
		}
	}
	CXProduct.prototype.forEach = CXProduct.prototype.forEach1;//	portions from http://phrogz.net/lazy-cartesian-product
	
	
	function getFunctionArgs(f) {
		var str = f+"";
		var start = str.indexOf("(")+1;
		var end = str.indexOf(")");
		var result = str.substring(start,end).split(",");
		result.forEach(function(arg,i) {
			result[i] = arg.trim();
		});
		if(result.length===1 && result[0]==="") {
			return [];
		}
		return result;
	}
	function compile(reactor,rule,boost) {
		var variables = Object.keys(rule.domain);
		variables.forEach(function(variable) {
			var cons = rule.domain[variable];
			if(typeof(cons)!=="function") {
				throw new TypeError("Domain variable " + variable + " is not a constructor in rule " + rule.name);
			}
			if(!cons.name || cons.name.length===0 || cons.name==="anonymous") {
				throw new TypeError("Constructor for domain variable " + variable + " in rule " + rule.name + " must have a name.");
			}
			if(!rule.reactor.domain[cons.name]) {
				rule.reactor.domain[cons.name] = cons;
			}
			cons.instances = (cons.instances ? cons.instances : []);
			cons.index = (cons.index ? cons.index : {});
			cons.prototype.rules = (cons.prototype.rules ? cons.prototype.rules : {});
			cons.prototype.rules[rule.name] = rule;
			cons.prototype.activeKeys = (cons.prototype.activeKeys ? cons.prototype.activeKeys : {});
			cons.exists = function(f) {
				f = (f ? f : function() { return true; });
				return cons.instances && cons.instances.some(function(instance) {
					return f(instance);
				});
			};
			cons.forAll = function(f) {
				return cons.instances && cons.instances.every(function(instance) {
					return f(instance);
				});
			};
			rule.range[variable] = {};
			rule.bindings[variable] = (rule.bindings[variable] ? rule.bindings[variable] : []);
			// extract instance keys from condition using a side-effect of replace
			rule.conditions.forEach(function(condition) {
				(condition+"").replace(new RegExp("(\\b"+variable+"\\.\\w+\\b)","g"),
					function(match) { 
						var parts = match.split("."),key = parts[1];
						// cache reactive non-function keys on class prototype
						if(key.indexOf("(")===-1) {
							cons.prototype.activeKeys[key] = true;
							// cache what keys are associated with what variables
							rule.range[variable][key] = true;
						}
						// don't really do a replace!
						return match;
					}
				);
			});
		});
		if(variables.length>0) {
			rule.triggers.push({domain:rule.domain,range:rule.range});
		}
		rule.conditions.forEach(function(condition) {
			(condition+"").replace(/exists\(\s*(\s*{.*\s*})\s*,\s*(.*)\s*\)/g,
				function(match,domainstr,conditionstr) {
					var domain = new Function("return " + domainstr)(), variables = Object.keys(domain);
					var quantification = {domain: domain, range: {}};
					rule.triggers.push(quantification);
					variables.forEach(function(variable) {
						var cons = domain[variable];
						if(!rule.reactor.domain[cons.name]) {
							rule.reactor.domain[cons.name] = cons;
						}
						quantification.range[variable] = (quantification.range[variable] ? quantification.range[variable] : {});
						cons.prototype.rules = (cons.prototype.rules ? cons.prototype.rules : {});
						cons.prototype.rules[rule.name] = rule;
						cons.prototype.activeKeys = (cons.prototype.activeKeys ? cons.prototype.activeKeys : {});
						conditionstr.replace(new RegExp("(\\b"+variable+"\\.\\w+\\b)","g"),
							function(match) { 
								var parts = match.split("."),key = parts[1];
								// cache reactive non-function keys on class prototype
								if(key.indexOf("(")===-1) {
									cons.prototype.activeKeys[key] = true;
									// cache what keys are associated with what variables
									quantification.range[variable][key] = true;
								}
								// don't really do a replace!
								return match;
							}
						);
					});
					// don't really do a replace!
					return match;
				}
			);
			(condition+"").replace(/forAll\(\s*(\s*{.*\s*})\s*,\s*(.*)\s*\)/g,
					function(match,domainstr,conditionstr) {
						var domain = new Function("return " + domainstr)(), variables = Object.keys(domain);
						var quantification = {domain: domain, range: {}};
						rule.triggers.push(quantification);
						variables.forEach(function(variable) {
							var cons = domain[variable];
							if(!rule.reactor.domain[cons.name]) {
								rule.reactor.domain[cons.name] = cons;
							}
							quantification.range[variable] = (quantification.range[variable] ? quantification.range[variable] : {});
							cons.prototype.rules = (cons.prototype.rules ? cons.prototype.rules : {});
							cons.prototype.rules[rule.name] = rule;
							cons.prototype.activeKeys = (cons.prototype.activeKeys ? cons.prototype.activeKeys : {});
							conditionstr.replace(new RegExp("(\\b"+variable+"\\.\\w+\\b)","g"),
								function(match) { 
									var parts = match.split("."),key = parts[1];
									// cache reactive non-function keys on class prototype
									if(key.indexOf("(")===-1) {
										cons.prototype.activeKeys[key] = true;
										// cache what keys are associated with what variables
										quantification.range[variable][key] = true;
									}
									// don't really do a replacereactornt!
									return match;
								}
							);
						});
						// don't really do a replacereactornt!
						return match;
					}
				);
		});
		rule.compiledConditions = [];
		rule.conditions.forEach(function(condition,i) {
			if((condition+"").indexOf("return")===-1) {
				throw new TypeError("Condition function missing a return statereactornt in rule '" + rule.name + "' condition " + i);
			}
			var args = getFunctionArgs(condition);
			condition.required = new Array(args.length);
			args.forEach(function(arg,j) {
				var required = variables.indexOf(arg);
				if(required===-1) {
					throw new ReferenceError("Referenced domain variable '" + arg + "' undefined in rule '" + rule.name + "' condition ",i);
				}
				condition.required[j] = required;
			});
			if(!boost) {
				rule.compiledConditions.push(function(match) {
					var reactor = this, args = [];
					// no required = domainless
					if(!condition.required || condition.required.every(function(i) {
						if(typeof(match[i])!=="undefined") {
							return args.push(match[i]);
						}
					})) {
						return condition.apply(reactor,args);
					}
					return true;
				});
			} else {
				var required = (condition.required ? condition.required : []);
				var f = new Function("match","var args = [];if(" + JSON.stringify(required) + ".every(function(i,j,required){if(match[i]!==undefined){return args.push(match[i]);}})) {return (" + condition + ").apply(undefined,args);}");
				rule.compiledConditions.push(f);
			}
		});
		var args = getFunctionArgs(rule.action);
		rule.action.required = new Array(args.length);
		args.forEach(function(arg,i) {
			var required = variables.indexOf(arg);
			if(required===-1) {
				throw new ReferenceError("Referenced domain variable '" + arg + "' undefined in rule '" + rule.name + "' action");
			}
			rule.action.required[i] = required;
			
		});
		// do not add full compilation since actions are allowed to use closure scope and it will break that
		rule.compiledAction = function(match) {
			var reactor = this, args = [];
			// no required = domainless
			if(!rule.action.required || rule.action.required.every(function(i) {
				if(typeof(match[i])!=="undefined") {
					return args.push(match[i]);
				}
			})) {
				rule.action.apply(reactor,args);
			}
		};
	}

	// dummy console so logging can easily be retractd
	var Console = {};
	Console.log = function() { 
		var c = console;
		c.log.apply(console,arguments); 
	};
	// uncomment line below to stop logging
	//Console.log = function() {};
	
	// http://stackoverflow.com/questions/1344500/efficient-way-to-insert-a-number-into-a-sorted-array-of-numbers
	function insertSorted(arr, item, comparator) {
		if (comparator == null) {
			// emulate the default Array.sort() comparator
			comparator = function(a, b) {
				if (typeof a !== "string") { a = String(a); }
				if (typeof b !== "string") { b = String(b); }
				return (a > b ? 1 : (a < b ? -1 : 0));
			};
		}
		// get the index we need to insert the item at
		var min = 0;
		var max = arr.length;
		var index = Math.floor((min + max) / 2);
		while (max > min) {
			if (comparator(item, arr[index]) < 0) {
				max = index;
			} else {
				min = index + 1;
			}
			index = Math.floor((min + max) / 2);
		}
		// insert the item
		arr.splice(index, 0, item);
	}

	function Activation(rule,match,index,bindings,instance) {
		var me = this;
		me.timestamp = new Date();
		me.rule = rule;
		me.match = match;
		me.index = index;
		me.bindings = bindings;
		me.instance = instance;
		me.rule.activated++;
		insertSorted(me.rule.reactor.agenda,me,function(a,b) { return (a.rule.salience > b.rule.salience ? 1 : (a.rule.salience < b.rule.salience ? -1 : 0)); });
		var activations = rule.activations.get(instance);
		if(!activations) {
			activations = [];
		}
		activations.push(me);
		rule.activations.set(instance,activations);
		if(me.rule.reactor.tracelevel>1) {
			Console.log("Activating: ",rule,match);
		}
	}
	Activation.prototype.cancel = function() {
		this.cancelled = true;
	}
	Activation.prototype.execute = function(index) {
		var me = this;
		// re-test just in-case
		me.delete(undefined,index,true);
		if(!me.cancelled && (!me.bindings || me.bindings.verify(me.index,me.match)) && me.rule.compiledConditions.every(function(condition) {
			return condition.call(me,me.match);
		})) {
			me.rule.fire(me.match);
		}
	}
	Activation.prototype.delete = function(instance,index,supresslog) {
		var me = this, i;
		if(!instance || me.match.indexOf(instance)>=0) {
			if(!supresslog && me.rule.reactor.tracelevel>1) {
				Console.log("Deactivating: ",me.rule,me.match);
			}
			var activations = me.rule.activations.get(instance);
			if(activations) {
				i = activations.indexOf(me);
				if(i>=0) {
					activations.splice(i,1);
					if(activations.length===0) {
						me.rule.activations.delete(instance);
					}
				}
			}
			if(typeof(index)!=="undefined") {
				if(index===me.rule.reactor.agenda.length-1) {
					me.rule.reactor.agenda.pop();
					return;
				}
				me.rule.reactor.agenda.splice(index,1);
				return;
			}
			i = me.rule.reactor.agenda.indexOf(me);
			if(i>=0) {
				me.rule.reactor.agenda.splice(i,1);
			}
		}
	}

	function Rule(reactor,name,salience,domain,condition,action,boost) {
		var me = this;
		me.boost = boost;
		me.name = name;
		me.reactor = reactor;
		if(typeof(salience)!=="number") {
			throw new TypeError("Salience " + salience + " is not a number in rule " + name);
		}
		me.salience = salience;
		me.domain = domain;
		if(typeof(domain)!=="object") {
			throw new TypeError("Domain " + domain + " is not an object in rule " + name);
		}
		me.range = {};
		me.triggers = [];
		me.conditions = (Array.isArray(condition) || condition instanceof Array ? condition : [condition]);
		me.pattern = new Array(Object.keys(domain).length);
		if(typeof(action)!=="function") {
			throw new TypeError("Action " + action + " is not a function in rule " + name);
		}
		me.action = action;
		me.bindings = {};
		me.activations = new Map();
		me.potentialMatches = 0;
		me.tested = 0;
		me.activated = 0;
		me.fired = 0;
		compile(reactor,me,me.boost);
		if(me.reactor.tracelevel>2) {
			Console.log("New Rule: ",me);
		}
		me.bindInstances(true);
	} 
	Rule.prototype.bind = function(instance,test) {
		var me = this, variables = Object.keys(me.bindings);
		variables.map(function(variable) {
			if(instance instanceof me.domain[variable] && me.bindings[variable].indexOf(instance)===-1) {
				if(me.reactor.tracelevel>2) {
					Console.log("Binding: ",me,variable,instance);
				}
				me.bindings[variable].push(instance);
			}
		});
		if(test) {
			return me.test(instance);
		}
	}
	Rule.prototype.bindInstances = function(test) {
		var me = this, variables = Object.keys(me.domain);
		variables.forEach(function(variable) {
			me.domain[variable].instances.forEach(function(instance) {
				me.bind(instance,test);
			});
		});
	}
	Rule.prototype.delete = function() {
		var me = this, variables = Object.keys(me.domain);
		variables.forEach(function(variable) {
			var cons = me.domain[variable];
			delete cons.prototype.rules[me.name];
		});
	}
	Rule.prototype.fire = function(match) {
		var me = this;
		if(me.reactor.tracelevel>0) {
			Console.log("Firing: ",this,match);
		}
		this.fired++;
		this.reactor.run.executions++;
		this.compiledAction(match);
	}
	Rule.prototype.test = function(instance,key) {
		var me = this, variables = Object.keys(me.domain), result = false, values = [];
		if(!variables.every(function(variable) {
			values.push(me.bindings[variable]);
			return me.bindings[variable].length>0;
		})) {
			me.cxproduct = null;
			return false;
		}
		
		if(!me.cxproduct) {
			me.cxproduct = new CXProduct(values);
		}
		if(me.reactor.tracelevel>2) {
			Console.log("Testing: ",me,instance,key);
		}
		me.tested++;
		var test = function (match,i) {
			if(me.compiledConditions.every(function(condition) {
				return condition.call(me,match);
			})) {
				new Activation(me,match,i,me.cxproduct,instance);
				result = true;
				return true;
			}
		}
		if(variables.length===0) {
			if(test()) {
				result = true;
			} else {
				me.reset(instance);
				result = false;
			}
		} else {
			me.potentialMatches = Math.max(me.potentialMatches,me.cxproduct.length());
			if(instance) {
				variables.forEach(function(variable,i) {
					var collections = me.cxproduct.collections.slice(0);
					if(instance instanceof me.domain[variable]) {
						collections[i] = [instance];
						var cxproduct = new CXProduct(collections);
						test = function (match,i) {
							if(me.compiledConditions.every(function(condition) {
								return condition.call(me,match);
							})) {
								new Activation(me,match,i,cxproduct,instance);
								result = true;
							}
						}
						cxproduct.forEach(test);
					}
				});
			} else {
				me.cxproduct.forEach(test);
			}
		}
		return result;
	}
	Rule.prototype.reset = function(instance) {
		var me = this;
		if(me.reactor.tracelevel>2) {
			Console.log("Reseting: ",me,instance);
		}
		me.activations.forEach(function(activations,activator) {
			if(!instance || activator===instance) {
				activations.forEach(function(activation) {
					var i = me.reactor.agenda.indexOf(activation);
					if(i>=0) {
						me.reactor.agenda.splice(i,1);
					}
				});
				
			}
		});
	}
	
	Rule.prototype.unbind = function(instance) {
		var me = this, variables = Object.keys(me.bindings);
		variables.map(function(variable) {
			if(instance instanceof me.domain[variable]) {
				var i = me.bindings[variable].indexOf(instance);
				if(i>=0) {
					if(me.reactor.tracelevel>2) {
						Console.log("Unbinding: ",me,variable,instance);
					}
					me.bindings[variable].splice(i,1);
					if(me.bindings[variable].length===0) {
						me.cxproduct = null;
					}
					me.reset(instance);
				}
			}
		});
	}

	function indexObject(index,instance) {
		var keys, primitive = false;
		if(instance instanceof Number || instance instanceof String || instance instanceof Boolean) {
			keys = ["value"];
			primitive = true;
		} else {
			keys = Object.keys(instance);
		}	
		keys.forEach(function(key) {
			index[key] = (index[key] ? index[key] : {});
			var value = (primitive ? instance.valueOf() : instance[key]), type = typeof(value), valuekey, typekey;
			if(type==="object" && value) {
				if(typeof(value.__rrid__)==="undefined") {
					Object.defineProperty(value,"__rrid__",{value:uuid.v4()});
				}
				valuekey = value.constructor.name + "@" + value.__rrid__;
			} else {
				valuekey = value;
			}
			if(value===null || typeof(value)==="undefined") {
				typekey = "undefined";
			} else {
				typekey = type;
			}
			index[key][valuekey] = (index[key][valuekey] ? index[key][valuekey] : {});
			index[key][valuekey][typekey] = (index[key][valuekey][typekey] ? index[key][valuekey][typekey] : {});
			index[key][valuekey][typekey][instance.__rrid__] = instance;
		});
	}
	function updateIndex(index,instance,key,oldValue) {
		if(typeof(instance[key])==="undefined" && !index[key]) { return; }
		index[key] = (index[key] ? index[key] : {});
		var value = instance[key], type = typeof(value), oldtype = typeof(oldValue), oldvaluekey, oldtypekey, valuekey, typekey;
		if(type==="object" && value) {
			if(typeof(value.__rrid__)==="undefined") {
				Object.defineProperty(value,"__rrid__",{value:uuid.v4()});
			}
			valuekey = value.constructor.name + "@" + value.__rrid__;
		} else {
			valuekey = value;
		}
		if(value===null || typeof(value)==="undefined") {
			typekey = "undefined";
		} else {
			typekey = type;
		}
		if(oldtype==="object" && oldValue) {
			oldvaluekey = oldValue.constructor.name + "@" + oldValue.__rrid__;
		} else {
			oldvaluekey = oldValue;
		}
		if(value===null || typeof(value)==="undefined") {
			typekey = "undefined";
		} else {
			typekey = type;
		}
		if(oldValue===null || typeof(oldValue)==="undefined") {
			oldtypekey = "undefined";
		} else {
			oldtypekey = oldtype;
		}
		index[key][valuekey] = (index[key][valuekey] ? index[key][valuekey] : {});
		index[key][valuekey][typekey] = (index[key][valuekey][typekey] ? index[key][valuekey][typekey] : {});
		index[key][valuekey][typekey][instance.__rrid__] = instance;
		if(index[key][oldvaluekey] && index[key][oldvaluekey][oldtypekey]) {
			delete index[key][oldvaluekey][oldtypekey][instance.__rrid__];
		}
	}
	function matchObject(index,instance,parentkeys,parentinstances) {
		if(!index) { return false; }
		var	keys, primitive = false, instances;
		if(instance instanceof Number || instance instanceof String || instance instanceof Boolean || ["number","string","boolean"].indexOf(typeof(instance))>=0) {
			keys = ["value"];
			primitive = true;
		} else {
			keys = Object.keys(instance);
		}	
		parentkeys = (parentkeys ? parentkeys : []);
		parentinstances = (parentinstances ? parentinstances : []);
		return keys.every(function(key) {
			if(!primitive && !index[key]) { return false; }
			var value = (primitive ? instance.valueOf() : instance[key]), type = typeof(value), valuekey, typekey;
			if(type==="object" && value) {
				if(parentkeys.indexOf(key)>=0 && parentkeys.indexOf(key)===parentinstances.indexOf(value)) {
					return true;
				}
				var valuekeys = Object.keys(index[key]);
				return valuekeys.some(function(valuekey) {
					var parts = valuekey.split("@");
					if(parts.length!==2) {
						return false;
					}
					var cons = Function("return " + parts[0])();
					if(typeof(cons)!=="function" || !cons.index) {
						return false;
					}
					parentkeys.push(key);
					parentinstances.push(value);
					return matchObject(cons.index,value,parentkeys,parentinstances);
				});
			} else {
				valuekey = value;
			}
			if(value===null || typeof(value)==="undefined") {
				typekey = "undefined";
			} else {
				typekey = type;
			}
			if(!index[key][valuekey]) { return false; }
			if(!index[key][valuekey][typekey]) { return false; }
			if(instance.__rrid__ && !index[key][valuekey][typekey][instance.__rrid__]){  return false; }
			instances = (instances ? intersection(instances,Object.keys(index[key][valuekey][typekey]))  : Object.keys(index[key][valuekey][typekey]));
			return instances.length>0;
		});
	}
	function notMatchObject(index,instance,parentkeys,parentinstances) {
		if(!index) { return false; }
		var	primitive = false;
		if(instance instanceof Number || instance instanceof String || instance instanceof Boolean || ["number","string","boolean"].indexOf(typeof(instance))>=0) {
			primitive = true;
		}	
		parentkeys = (parentkeys ? parentkeys : []);
		parentinstances = (parentinstances ? parentinstances : []);
		return Object.keys(index).some(function(key) {
			var value = (primitive ? instance.valueOf() : instance[key]), type = typeof(value);
			if(!primitive && typeof(instance[key])==="undefined") {
				return false;
			}
			if(type==="object" && value) {
				if(parentkeys.indexOf(key)>=0 && parentkeys.indexOf(key)===parentinstances.indexOf(value)) {
					return true;
				}
				var valuekeys = Object.keys(index[key]);
				return valuekeys.some(function(valuekey) {
					var parts = valuekey.split("@");
					if(parts.length!==2) {
						return false;
					}
					var cons = Function("return " + parts[0])();
					if(typeof(cons)!=="function" || !cons.index) {
						return false;
					}
					parentkeys.push(key);
					parentinstances.push(value);
					return notMatchObject(cons.index,value,parentkeys,parentinstances);
				});
			}
			return Object.keys(index[key]).some(function(valuekey) {
				if(valuekey!==value+"") {
					return true;
				}
				return Object.keys(index[key][valuekey]).some(function(typekey) {
					if(typekey!==type) {
						return true;
					}
				});
			});
		});
	}
	function RuleReactor (domain,boost) {
		this.boost = boost;
		this.rules = {};
		this.triggerlessRules = {};
		this.data = new Map();
		this.agenda = [];
		this.run.assertions = 0;
		this.run.modifications = 0;
		this.run.executions = 0;
		this.domain = (domain ? domain : {});
	}
	RuleReactor.prototype.assert = function(instances,callback) {
		var me = this;
		// add instance to class.constructor.instances
		instances = (Array.isArray(instances) || instances instanceof Array ? instances : [instances]);
		var instancestoprocess = [];
		instances.forEach(function(instance) {
			// don't bother processing instances that don't impact rules or are already in the data store
			if(instance && typeof(instance)==="object") { // !RuleReactor.data.has(instance)
				if(typeof(instance.__rrid__)==="undefined") {
					Object.defineProperty(instance,"__rrid__",{value:uuid.v4()});
				}
				if(me.data.has(instance.__rrid)) {
					return;
				}
				if(me.tracelevel>2) {
					Console.log("Assert: ",instance);
				}
				me.run.assertions++;
				me.data.set(instance.__rrid__,instance);
				me.dataModified = true;
				instancestoprocess.push(instance);
				instance.constructor.instances = (instance.constructor.instances ? instance.constructor.instances : []);
				instance.constructor.instances.push(instance);
				instance.constructor.index = (instance.constructor.index ? instance.constructor.index : {});
				indexObject(instance.constructor.index,instance);
				// patch any keys on instance or those identified as active while compiling
				var keys = Object.keys(instance);
				if(instance.activeKeys) {
					Object.keys(instance.activeKeys).forEach(function(key) {
						if(keys.indexOf(key)===-1) {
							keys.push(key);
						}
					});
				}
				keys.forEach(function(key) {
					function rrget() {
						return rrget.value;
					}
					function rrset(value) {
						if(rrget.value!==value) {
							if(me.tracelevel>2) {
								Console.log("Modify: ",instance,key,rrget.value,"=>",value);
							}
							var oldvalue = rrget.value;
							// set new value
							rrget.value = value;
							updateIndex(instance.constructor.index,instance,key,oldvalue);
							me.dataModified = true; // may not have an impact
							me.run.modifications++;
							// if the value is an object that has possible rule matches, assert it
							if(value && value.rules) {
								me.assert(value);
							}
							// re-test the rules that pattern match the key
							if(instance.rules) { 
								Object.keys(instance.rules).forEach(function(rulename) {
									var rule = instance.rules[rulename];
									if(rule.triggers.some(function(trigger) {
										return Object.keys(trigger.range).some(function(variable) {
											return trigger.range[variable][key] && instance instanceof trigger.domain[variable];
									}); })) {
										var activations = rule.activations.get(instance);
										if(activations) {
											activations.forEach(function(activation) {
												activation.cancel();
											});
										}
										rule.test(instance,key);
									}
								});	
							}
							return rrget.value;
						}
					}
					var desc = Object.getOwnPropertyDescriptor(instance,key);
					var originalDescriptor;
					if(desc) {
						originalDescriptor = {};
						Object.keys(desc).forEach(function(key) {
							originalDescriptor[key] = desc[key];
						});
					}
					// create a new descriptor if one does not exist
					desc = (desc ? desc : {enumerable:true,configurable:false});
					if(!desc.get || desc.get.name!=="rrget") {
						// rrget existing value
						rrget.value = desc.value;
						rrget.originalDescriptor = originalDescriptor;
						// modify arrays
						if(desc.value instanceof Array || Array.isArray(desc.value)) {
							var value = desc.value;
							originalDescriptor.value = value.slice();
							var modifiers = ["push","pop","splice","shift","unshift"];
							modifiers.forEach(function(fname) {
								var f = value[fname];
								if(typeof(f)==="function") {
									var newf = function() {
										f.apply(value,arguments);
										me.run.modifications++;
										// re-test the rules that pattern match the key
										Object.keys(instance.rules).forEach(function(rulename) {
											var rule = instance.rules[rulename];
											if(rule.triggers.some(function(trigger) {
												return Object.keys(trigger.range).some(function(variable) {
													return trigger.range[variable][key] && instance instanceof trigger.domain[variable];
											});})) {
												var activations = rule.activations.get(instance);
												if(activations) {
													activations.forEach(function(activation) {
														activation.cancel();
													});
												}
												rule.test(instance,key);
											}
										});
									}
									Object.defineProperty(rrget.value,fname,{configurable:true,writable:true,value:newf});
								}
							});
							Object.getOwnPropertyNames(Array.prototype).forEach(function(fname) {
								var f = value[fname];
								if(typeof(f)==="function" && modifiers.indexOf(fname)===-1) {
									Object.defineProperty(rrget.value,fname,{configurable:true,writable:true,value:function() {
										return f.apply(value,arguments);
									}});
								}
							});
						}
						// delete descriptor properties that are inconsistent with rrget/rrset
						delete desc.value;
						delete desc.writable;
						desc.get = rrget;
						desc.set = rrset;
						Object.defineProperty(instance,key,desc);
						
					}
				});
			}
		});
		instancestoprocess.forEach(function(instance) {
			if(instance.rules) {
				Object.keys(instance.rules).forEach(function(ruleinstance) {
					instance.rules[ruleinstance].bind(instance);
				});
			}
		});
		// test all associated rules
		instancestoprocess.forEach(function(instance) {
			if(instance.rules) {
				Object.keys(instance.rules).forEach(function(rulename) {
					var rule = instance.rules[rulename];
					rule.test(instance);
				});
			}
		});
		if(callback) {
			callback(null);
		}
	}
	RuleReactor.prototype.createRule = function(name,salience,domain,condition,action) {
		var me = this, rule = new Rule(this,name,salience,domain,condition,action,me.boost);
		me.rules[rule.name] = rule;
		if(rule.triggers.length===0) {
			me.triggerlessRules[rule.name] = rule;
		}
		return rule;
	}
	RuleReactor.forAll = function forAll(domain,test) {
		if(typeof(domain)!=="object") {
			throw new TypeError("Domain " + domain + " is not an object in universal quantification");
		}
		var variables = Object.keys(domain);
		if(typeof(test)==="object") {
			if(!test) {
				throw new TypeError("Universal quantification condition is null");
			}
			return Object.keys(test).every(function(variable) {
				var i = variables.indexOf(variable);
				if(i===-1) {
					throw new ReferenceError("Undeclared domain variable '" + variable + "' in existential quantification match condition");
				}
				return !notMatchObject(domain[variable].index,test[variable]);
			});
		}
		if((test+"").indexOf("return ")===-1) {
			throw new TypeError("Universal quantification condition function missing a return statement: " + test);
		}
		if(!test.cxproduct) {
			var collections = [], args;
			variables.forEach(function(variable) {
				domain[variable].instances = (domain[variable].instances ? domain[variable].instances: []);
				collections.push(domain[variable].instances);
			});
			test.cxproduct = new CXProduct(collections);
			args = getFunctionArgs(test);
			if(args.length>0) {
				test.required = args.map(function(variable) { 
					var i = variables.indexOf(variable);
					if(i===-1) {
						throw new ReferenceError("Undeclared domain variable '" + variable + "' in universal quantification condition function");
					}
					return i; 
				});
			}
		}
		return test.cxproduct.every(function(row) {
			var args = [];
			if(!test.required || test.required.every(function(index) { args.push(row[index]); return typeof(row[index])!=="undefined"; })) {
				return test.apply(null,args);
			}
		});
	}
	RuleReactor.prototype.forAll  = RuleReactor.forAll;
	RuleReactor.exists = function exists(domain,test) {
		if(typeof(domain)!=="object") {
			throw new TypeError("Domain " + domain + " is not an object in existential quantification");
		}
		var variables = Object.keys(domain);
		if(typeof(test)==="object") {
			if(!test) {
				throw new TypeError("Existential quantification condition is null");
			}
			return Object.keys(test).every(function(variable) {
				var i = variables.indexOf(variable);
				if(i===-1) {
					throw new ReferenceError("Undeclared domain variable '" + variable + "' in existential quantification match condition");
				}
				return matchObject(domain[variable].index,test[variable]);
			});
		}
		if((test+"").indexOf("return ")===-1) {
			throw new TypeError("Existential quantification condition function missing a return statement: " + test);
		}
		if(!test.cxproduct) {
			var collections = [], args;
			variables.forEach(function(variable) {
				domain[variable].instances = (domain[variable].instances ? domain[variable].instances: []);
				collections.push(domain[variable].instances);
			});
			test.cxproduct = new CXProduct(collections);
			args = getFunctionArgs(test);
			if(args.length>0) {
				test.required = args.map(function(variable) { 
					var i = variables.indexOf(variable);
					if(i===-1) {
						throw new ReferenceError("Undeclared domain variable '" + variable + "' in existential quantification condition function");
					}
					return i; 
				});
			}
		}
		return test.cxproduct.some(function(row) {
			var args = [];
			if(!test.required || test.required.every(function(index) { args.push(row[index]); return typeof(row[index])!=="undefined"; })) {
				return test.apply(null,args);
			}
		});
	}
	RuleReactor.prototype.exists = RuleReactor.exists;
	RuleReactor.not = function not(value) {
		return !value;
	}
	RuleReactor.prototype.not = RuleReactor.not;
	RuleReactor.prototype.retract = function(instances,run) {
		var me = this;
		instances = (Array.isArray(instances) || instances instanceof Array ? instances : [instances]);
		instances.forEach(function(instance) {
			if(instance && typeof(instance)==="object") {
				if(me.tracelevel>2) {
					Console.log("Retract: ",instance);
				}
				// retract from data
				me.data.delete(instance.__rrid__);
				me.dataModified = true;
				// restore instance properties
				Object.keys(instance).forEach(function(key) {
					var desc = Object.getOwnPropertyDescriptor(instance,key);
					if(desc.get && desc.get.name==="rrget") {
						if(typeof(desc.get.originalDescriptor)==="undefined") {
							delete instance[key];
						} else {
							if(desc.get.originalDescriptor.value instanceof Array || Array.isArray(desc.get.originalDescriptor.value)) {
								if(instance[key] instanceof Array || Array.isArray(instance[key])) {
									var args = [0,instance[key]].concat(desc.get.originalDescriptor.value);
									instance[key].splice.apply(instance[key],args);
								} else {
									instance[key] = desc.get.originalDescriptor.value;
								}
							}
							Object.defineProperty(instance,key,desc.get.originalDescriptor);
						}
					}
				});
				// unbind from all associated rules
				Object.keys(instance.rules).forEach(function(rulename) {
					instance.rules[rulename].unbind(instance);
				});

			}
		});
		if(run) {
			setTimeout(function() { me.run(); });
		}
	}
	RuleReactor.prototype.reset = function(facts) {
		var me = this;
		Object.keys(me.rules).forEach(function(rulename) {
			me.rules[rulename].reset();
		});
		if(facts) {
			var data = [];
			me.data.forEach(function(instance) {
				data.push(instance);
			});
			me.retract(data,false);
		}
		me.run.running = false;
	}
	RuleReactor.prototype.run = function(max,loose,callback) {
		var me = this;
		function run() {
			if(me.run.stop) {
				if(me.tracelevel>0) {
					Console.log("Data Count: ",me.data.size);
					Console.log("Executions: ",me.run.executions);
					Console.log("RPS: ",me.run.rps);
				}
				if(me.tracelevel>1) {
					Object.keys(me.rules).forEach(function(rulename) {
						var rule = me.rules[rulename];
						Console.log(rule.name,rule.potentialMatches,rule.tested,rule.activated,rule.fired);
					});
				}
				if(typeof(callback)==="function") {
					callback();
				}
				return;
			}
			if(me.run.executions<me.run.max) {
				Object.keys(me.triggerlessRules).forEach(function(rulename) {
					var rule = me.triggerlessRules[rulename], activations = rule.activations.get(); // get's activations associated with undefined domains
					if(!activations || activations.length===0) {
						rule.test();
					}
				});
				while (me.agenda.length>0) {
					me.dataModified = false;
					me.agenda[me.agenda.length-1].execute(me.agenda.length-1);
					if(me.dataModified) {
						break;
					}
				}
			}
			setTimeout(run,0);
		}
		if(me.run.running) { return true; }
		me.run.max = (max ? max : Infinity);
		me.run.running = true;
		me.run.executions = 0;
		me.run.assertions = 0;
		me.run.modifications = 0;
		me.run.start = new Date();
		me.run.stop = null;
		if(me.tracelevel>0) {
			Console.log("Run: ",max);
		}
		setTimeout(run,0);
	}
	RuleReactor.prototype.stop = function() {
		this.run.stop = new Date();
		this.run.rps = (this.run.executions / (this.run.stop.getTime() - this.run.start.getTime())) * 1000;
		this.run.running = false;
	}
	RuleReactor.prototype.trace = function(level) {
		this.tracelevel = level;
		if(this.processor) {
			if(level>3) {
				this.processor.trace(1);
			} else {
				this.processor.trace(0);
			}
		}	
	}

	if (this.exports) {
		this.exports  = RuleReactor;
	} else if (typeof define === "function" && define.amd) {
		// Publish as AMD module
		define(function() {return RuleReactor;});
	} else {
		this.RuleReactor = RuleReactor;
	}
}).call((typeof(window)!=="undefined" ? window : (typeof(module)!=="undefined" ? module : null)));


},{"uuid":3}],2:[function(require,module,exports){
(function (global){

var rng;

if (global.crypto && crypto.getRandomValues) {
  // WHATWG crypto-based RNG - http://wiki.whatwg.org/wiki/Crypto
  // Moderately fast, high quality
  var _rnds8 = new Uint8Array(16);
  rng = function whatwgRNG() {
    crypto.getRandomValues(_rnds8);
    return _rnds8;
  };
}

if (!rng) {
  // Math.random()-based (RNG)
  //
  // If all else fails, use Math.random().  It's fast, but is of unspecified
  // quality.
  var  _rnds = new Array(16);
  rng = function() {
    for (var i = 0, r; i < 16; i++) {
      if ((i & 0x03) === 0) r = Math.random() * 0x100000000;
      _rnds[i] = r >>> ((i & 0x03) << 3) & 0xff;
    }

    return _rnds;
  };
}

module.exports = rng;


}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{}],3:[function(require,module,exports){
//     uuid.js
//
//     Copyright (c) 2010-2012 Robert Kieffer
//     MIT License - http://opensource.org/licenses/mit-license.php

// Unique ID creation requires a high quality random # generator.  We feature
// detect to determine the best RNG source, normalizing to a function that
// returns 128-bits of randomness, since that's what's usually required
var _rng = require('./rng');

// Maps for number <-> hex string conversion
var _byteToHex = [];
var _hexToByte = {};
for (var i = 0; i < 256; i++) {
  _byteToHex[i] = (i + 0x100).toString(16).substr(1);
  _hexToByte[_byteToHex[i]] = i;
}

// **`parse()` - Parse a UUID into it's component bytes**
function parse(s, buf, offset) {
  var i = (buf && offset) || 0, ii = 0;

  buf = buf || [];
  s.toLowerCase().replace(/[0-9a-f]{2}/g, function(oct) {
    if (ii < 16) { // Don't overflow!
      buf[i + ii++] = _hexToByte[oct];
    }
  });

  // Zero out remaining bytes if string was short
  while (ii < 16) {
    buf[i + ii++] = 0;
  }

  return buf;
}

// **`unparse()` - Convert UUID byte array (ala parse()) into a string**
function unparse(buf, offset) {
  var i = offset || 0, bth = _byteToHex;
  return  bth[buf[i++]] + bth[buf[i++]] +
          bth[buf[i++]] + bth[buf[i++]] + '-' +
          bth[buf[i++]] + bth[buf[i++]] + '-' +
          bth[buf[i++]] + bth[buf[i++]] + '-' +
          bth[buf[i++]] + bth[buf[i++]] + '-' +
          bth[buf[i++]] + bth[buf[i++]] +
          bth[buf[i++]] + bth[buf[i++]] +
          bth[buf[i++]] + bth[buf[i++]];
}

// **`v1()` - Generate time-based UUID**
//
// Inspired by https://github.com/LiosK/UUID.js
// and http://docs.python.org/library/uuid.html

// random #'s we need to init node and clockseq
var _seedBytes = _rng();

// Per 4.5, create and 48-bit node id, (47 random bits + multicast bit = 1)
var _nodeId = [
  _seedBytes[0] | 0x01,
  _seedBytes[1], _seedBytes[2], _seedBytes[3], _seedBytes[4], _seedBytes[5]
];

// Per 4.2.2, randomize (14 bit) clockseq
var _clockseq = (_seedBytes[6] << 8 | _seedBytes[7]) & 0x3fff;

// Previous uuid creation time
var _lastMSecs = 0, _lastNSecs = 0;

// See https://github.com/broofa/node-uuid for API details
function v1(options, buf, offset) {
  var i = buf && offset || 0;
  var b = buf || [];

  options = options || {};

  var clockseq = options.clockseq !== undefined ? options.clockseq : _clockseq;

  // UUID timestamps are 100 nano-second units since the Gregorian epoch,
  // (1582-10-15 00:00).  JSNumbers aren't precise enough for this, so
  // time is handled internally as 'msecs' (integer milliseconds) and 'nsecs'
  // (100-nanoseconds offset from msecs) since unix epoch, 1970-01-01 00:00.
  var msecs = options.msecs !== undefined ? options.msecs : new Date().getTime();

  // Per 4.2.1.2, use count of uuid's generated during the current clock
  // cycle to simulate higher resolution clock
  var nsecs = options.nsecs !== undefined ? options.nsecs : _lastNSecs + 1;

  // Time since last uuid creation (in msecs)
  var dt = (msecs - _lastMSecs) + (nsecs - _lastNSecs)/10000;

  // Per 4.2.1.2, Bump clockseq on clock regression
  if (dt < 0 && options.clockseq === undefined) {
    clockseq = clockseq + 1 & 0x3fff;
  }

  // Reset nsecs if clock regresses (new clockseq) or we've moved onto a new
  // time interval
  if ((dt < 0 || msecs > _lastMSecs) && options.nsecs === undefined) {
    nsecs = 0;
  }

  // Per 4.2.1.2 Throw error if too many uuids are requested
  if (nsecs >= 10000) {
    throw new Error('uuid.v1(): Can\'t create more than 10M uuids/sec');
  }

  _lastMSecs = msecs;
  _lastNSecs = nsecs;
  _clockseq = clockseq;

  // Per 4.1.4 - Convert from unix epoch to Gregorian epoch
  msecs += 12219292800000;

  // `time_low`
  var tl = ((msecs & 0xfffffff) * 10000 + nsecs) % 0x100000000;
  b[i++] = tl >>> 24 & 0xff;
  b[i++] = tl >>> 16 & 0xff;
  b[i++] = tl >>> 8 & 0xff;
  b[i++] = tl & 0xff;

  // `time_mid`
  var tmh = (msecs / 0x100000000 * 10000) & 0xfffffff;
  b[i++] = tmh >>> 8 & 0xff;
  b[i++] = tmh & 0xff;

  // `time_high_and_version`
  b[i++] = tmh >>> 24 & 0xf | 0x10; // include version
  b[i++] = tmh >>> 16 & 0xff;

  // `clock_seq_hi_and_reserved` (Per 4.2.2 - include variant)
  b[i++] = clockseq >>> 8 | 0x80;

  // `clock_seq_low`
  b[i++] = clockseq & 0xff;

  // `node`
  var node = options.node || _nodeId;
  for (var n = 0; n < 6; n++) {
    b[i + n] = node[n];
  }

  return buf ? buf : unparse(b);
}

// **`v4()` - Generate random UUID**

// See https://github.com/broofa/node-uuid for API details
function v4(options, buf, offset) {
  // Deprecated - 'format' argument, as supported in v1.2
  var i = buf && offset || 0;

  if (typeof(options) === 'string') {
    buf = options === 'binary' ? new Array(16) : null;
    options = null;
  }
  options = options || {};

  var rnds = options.random || (options.rng || _rng)();

  // Per 4.4, set bits for version and `clock_seq_hi_and_reserved`
  rnds[6] = (rnds[6] & 0x0f) | 0x40;
  rnds[8] = (rnds[8] & 0x3f) | 0x80;

  // Copy bytes to buffer, if provided
  if (buf) {
    for (var ii = 0; ii < 16; ii++) {
      buf[i + ii] = rnds[ii];
    }
  }

  return buf || unparse(rnds);
}

// Export public API
var uuid = v4;
uuid.v1 = v1;
uuid.v4 = v4;
uuid.parse = parse;
uuid.unparse = unparse;

module.exports = uuid;

},{"./rng":2}]},{},[1]);
