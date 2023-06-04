function Worker(stringUrl) {
    this.url = stringUrl;
    this.onmessage = () => {};
}

Worker.prototype.postMessage = function(msg) {
    this.onmessage(msg);
}

if(typeof module!=='undefined'){module.exports=Worker;}