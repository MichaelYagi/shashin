(function (_T) {
    return (
        (_T.AssertionError = 3),
        _T
    );
}(window.testing = window.testing || {}));

function sum(a, b) {
    return a + b;
}

if (typeof module !== 'undefined') {
    module.exports = window.testing;
    //module.exports = sum;
}

// shashin.showToastMessage("Title 1", "Message 1.");
// shashin.showToastMessage("Title 2", "Message 2", {target:"liveToast1"});
// shashin.showToastMessage("Title 3", "Message 3", {autohide: false, target:"liveToast4", icon:"bi-alt", iconColor:"#ff0000"});
// shashin.showToastMessage("Title 4", "Message 5.", {autohide: false, target:"liveToast3", icon:"bi-exclamation-triangle", iconColor:"#000000"});



// const myelement = <h1>I Love JSX!</h1>;
//
// ReactDOM.render(myelement, document.getElementById('rnRoot'));