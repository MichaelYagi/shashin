function Worker(stringUrl) {
    this.url = stringUrl;
    this.onmessage = () => {};
}

Worker.prototype.postMessage = function(msg) {
    this.onmessage(msg);
}

module.exports = Worker