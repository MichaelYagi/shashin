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

shashin.showToastMessage("Title 1", "Message 1.",{autohide:false,placement:shashin.toast.placement.top.left});
shashin.showToastMessage("Title 2", "Message 2.",{autohide:false,placement:shashin.toast.placement.top.center});
shashin.showToastMessage("Title 3", "Message 3.",{autohide:false,placement:shashin.toast.placement.top.right});
shashin.showToastMessage("Title 4", "Message 4.",{autohide:false,placement:shashin.toast.placement.middle.left});
shashin.showToastMessage("Title 5", "Message 5.",{autohide:false,placement:shashin.toast.placement.middle.center});
shashin.showToastMessage("Title 6", "Message 6.",{autohide:false,placement:shashin.toast.placement.middle.right});
shashin.showToastMessage("Title 7", "Message 7.",{autohide:false,placement:shashin.toast.placement.bottom.left});
shashin.showToastMessage("Title 8", "Message 8.",{autohide:false,placement:shashin.toast.placement.bottom.center});
shashin.showToastMessage("Title 9", "Message 9.",{autohide:false,placement:shashin.toast.placement.bottom.right});

let options = {
    autohide: false
};
shashin.showToastMessage("title", "message", options);
options = {
    delay: 1000
};
shashin.showToastMessage("title1", "message1", options);
options = {
    autohide: false
};
shashin.showToastMessage("title2", "message2", options);

// shashin.closeToastMessage();

options = {
    delay: 1,
    placement: shashin.toast.placement.bottom.right
};
shashin.showToastMessage("title3", "message3", options);
options = {
    autohide: false,
    placement: shashin.toast.placement.bottom.right
};
shashin.showToastMessage("title5", "message5", options);
options = {
    placement: shashin.toast.placement.top.right
};
shashin.showToastMessage("title4", "message4", options);



const unreadNotifications = 12;
const notificationCount = 14;
const createdAtDate = "2025-01-01";
const title = notificationCount + " new notification" + (notificationCount === 1 ? "" : "s");
let message = '<div class="container"><strong>hello</strong></div>';

message = message + '</div>';

shashin.showToastMessage(title, message, {
    icon: "bi-bell",
    iconColor: "#FF8C00",
    headerSubtext: createdAtDate,
    autohide: false,
    borderColor:"warning"
});



// const myelement = <h1>I Love JSX!</h1>;
//
// ReactDOM.render(myelement, document.getElementById('rnRoot'));