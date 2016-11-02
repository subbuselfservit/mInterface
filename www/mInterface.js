window.mInterface = function(task,success,error) {
	cordova.exec(success, error, "mInterface", task, []);
};
