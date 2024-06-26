var exec = require('cordova/exec');

exports.getAllKeys = function (success, error) {
    exec(success, error, 'FirebaseRemoteConfigPlugin', 'getAllKeys');
};

exports.setConfigSettings = function (success, error) {
    exec(success, error, 'FirebaseRemoteConfigPlugin', 'setConfigSettings');
};