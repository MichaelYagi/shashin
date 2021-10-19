const {JSDOM} = require("jsdom");
const cfg = { url: "http://localhost" };
const dom = new JSDOM( "", cfg );
require('jsdom-global')()
const { Blob } = require('blob-polyfill');

global.window = dom.window;
global.document = dom.window.document;
global.Image = dom.window.Image;
global.Blob = Blob;
global.$ = global.jQuery = require('../../../../main/resources/static/js/jquery-3.5.1.min');
Object.keys( global.window ).forEach(( property ) => {
    if ( typeof global[ property ] === "undefined" ) {
        global[ property ] = global.window[ property ];
    }
});
function mockCanvas (window) {
    window.HTMLCanvasElement.prototype.getContext = function () {
        return {
            fillRect: function() {},
            clearRect: function(){},
            getImageData: function(x, y, w, h) {
                return  {
                    data: new Array(w*h*4)
                };
            },
            putImageData: function() {},
            createImageData: function(){ return []},
            setTransform: function(){},
            drawImage: function(){},
            save: function(){},
            fillText: function(){},
            restore: function(){},
            beginPath: function(){},
            moveTo: function(){},
            lineTo: function(){},
            closePath: function(){},
            stroke: function(){},
            translate: function(){},
            scale: function(){},
            rotate: function(){},
            arc: function(){},
            fill: function(){},
            measureText: function(){
                return { width: 0 };
            },
            transform: function(){},
            rect: function(){},
            clip: function(){},
        };
    }

    window.HTMLCanvasElement.prototype.toDataURL = function () {
        return "";
    }
}
mockCanvas(dom.window);