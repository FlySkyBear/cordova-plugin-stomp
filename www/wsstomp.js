//cordova.define("cordova-plugin-stomp.WSStomp", function(require, exports, module) {
	var exec = require('cordova/exec');

	function WSStomp(headers) {
		// make sure that the config object is valid
		if (typeof headers !== 'object') {
			throw {
				name : 'WSStomp Error',
				message : 'The first argument must be an object.'
			};
		}

		if ((typeof headers.headers === 'undefined') && (
				typeof headers.headers === 'undefined' && 
				( typeof headers.uri === 'undefined'
				|| typeof headers.login === 'undefined'))) {
			throw {
				name : 'WSStomp Error',
				message : 'isConnected, uri and uri are required parameters.'
			};
		}

		var self = this;
		self.events = {};
		self.headers = headers;

		// make all headers properties accessible from this object
		Object.keys(headers).forEach(function(prop) {
			Object.defineProperty(self, prop, {
				get : function() {
					return self.headers[prop];
				},
				set : function(value) {
					self.headers[prop] = value;
				}
			});
		});

		function callEvent(eventName) {
			if (!self.events[eventName]) {
				return;
			}

			var args = Array.prototype.slice.call(arguments, 1);
			self.events[eventName].forEach(function(callback) {
				callback.apply(self, args);
			});
		}

		function onMessage(data) {
			if (data.state === 'connected') {
				callEvent('connected', data);
			} else if (data.state === 'subscribed') {
				callEvent('subscribed', data);
			} else if (data.state === 'disconnected') {
				callEvent('disconnect');
			} else if (data.state === 'onMessage') {
				callEvent('onMessage', data.message);
			} else {
				console.log("unkown data=" + JSON.stringify(data));
			}
		}

		exec(onMessage, null, 'WSStompPlugin', 'openWSStomp', [ headers ]);
	}
	;

	WSStomp.prototype.on = function(eventName, fn) {
		// make sure that the second argument is a function
		if (typeof fn !== 'function') {
			throw {
				name : 'WSStomp Error',
				message : 'The second argument must be a function.'
			};
		}

		// create the event if it doesn't exist
		if (!this.events[eventName]) {
			this.events[eventName] = [];
		} else {
			// make sure that this callback doesn't exist already
			for (var i = 0, len = this.events[eventName].length; i < len; i++) {
				if (this.events[eventName][i] === fn) {
					throw {
						name : 'WSStomp Error',
						message : 'This callback function was already added.'
					};
				}
			}
		}

		// add the event
		this.events[eventName].push(fn);
	};

	WSStomp.prototype.off = function(eventName, fn) {
		// make sure that the second argument is a function
		if (typeof fn !== 'function') {
			throw {
				name : 'WSStomp Error',
				message : 'The second argument must be a function.'
			};
		}

		if (!this.events[eventName]) {
			return;
		}

		var indexesToRemove = [];
		for (var i = 0, len = this.events[eventName].length; i < len; i++) {
			if (this.events[eventName][i] === fn) {
				indexesToRemove.push(i);
			}
		}

		indexesToRemove.forEach(function(index) {
			this.events.splice(index, 1);
		})
	};

	WSStomp.prototype.connect = function(success, error) {
		exec(success, error, 'WSStompPlugin', 'connect', []);
	};
	WSStomp.prototype.wsConnect = function(success, error) {
		exec(success, error, 'WSStompPlugin', 'wsConnect', []);
	};
	WSStomp.prototype.subscribe = function(exchange) {
		exec(null, null, 'WSStompPlugin', 'subscribe', [ {
			exchange : exchange
		} ]);
	};

	WSStomp.prototype.send = function(destination, headers, body) {
		var data = {
				destination : destination,
				headers : headers,
				body : body
			}
		exec(null, null, 'WSStompPlugin', 'send', [ {
			sessionKey : this.sessionKey,
			message : data
		} ]);
	};

	WSStomp.prototype.disconnect = function() {
		exec(null, null, 'WSStompPlugin', 'disconnect', [ {
			sessionKey : this.sessionKey
		} ]);
	};

	exports.WSStomp = WSStomp;

//});
