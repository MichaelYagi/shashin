class Worker {
    constructor(stringUrl) {
        this.url = stringUrl;
        this.onmessage = () => {};
    }

    postMessage(msg) {
        this.onmessage(msg);
    }
}

// Used for JS tests
if(typeof module!=='undefined'){module.exports=Worker;}