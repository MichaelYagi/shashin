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

shashin.showToastMessage("Title 1", "Message 1.",{autohide:false,tag:"test1",placement:shashin.toast.placement.top.left});
shashin.showToastMessage("Title 2", "Message 2.",{autohide:false,tag:"test2",placement:shashin.toast.placement.top.center});
shashin.showToastMessage("Title 3", "Message 3.",{autohide:false,tag:"test3",placement:shashin.toast.placement.top.right});
shashin.showToastMessage("Title 4", "Message 4.",{autohide:false,tag:"test4",placement:shashin.toast.placement.middle.left});
shashin.showToastMessage("Title 5", "Message 5.",{autohide:false,tag:"test5",placement:shashin.toast.placement.middle.center});
shashin.showToastMessage("Title 6", "Message 6.",{autohide:false,tag:"test6",placement:shashin.toast.placement.middle.right});
shashin.showToastMessage("Title 7", "Message 7.",{autohide:false,tag:"test7",placement:shashin.toast.placement.bottom.left});
shashin.showToastMessage("Title 8", "Message 8.",{autohide:false,tag:"test8",placement:shashin.toast.placement.bottom.center});
shashin.showToastMessage("Title 9", "Message 9.",{autohide:false,tag:"test9",placement:shashin.toast.placement.bottom.right});
shashin.showToastMessage("Title 10", "Message 10.",{tag:"test10",placement:shashin.toast.placement.bottom.right});
shashin.showToastMessage("Title 11", "Message 11.",{autohide:false,tag:"test11",placement:shashin.toast.placement.bottom.right});


let timer = 3000;
setTimeout(function () {
    shashin.closeToastMessages({
        tags: ["test3"]
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages({
        tag: "test8"
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages({
        placements: ["topLeft"],
        tags: ["test4"]
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages({
        placement: "midCenter"
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages({
        placements: ["bottomRight", "midLeft"]
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages({
        tags: ["test5","test7"]
    });
}, timer);
timer += 2000;
setTimeout(function () {
    shashin.closeToastMessages();
}, timer);


// let options = {
//     autohide: false
// };
// shashin.showToastMessage("title", "message", options);
// options = {
//     delay: 1000
// };
// shashin.showToastMessage("title1", "message1", options);
// options = {
//     autohide: false
// };
// shashin.showToastMessage("title2", "message2", options);
//
// // shashin.closeToastMessages();
//
// options = {
//     delay: 1,
//     placement: shashin.toast.placement.bottom.right
// };
// shashin.showToastMessage("title3", "message3", options);
// options = {
//     autohide: false,
//     placement: shashin.toast.placement.bottom.right
// };
// shashin.showToastMessage("title5", "message5", options);
// options = {
//     placement: shashin.toast.placement.top.right
// };
// shashin.showToastMessage("title4", "message4", options);
//
//
//
// const unreadNotifications = 12;
// const notificationCount = 14;
// const createdAtDate = "2025-01-01";
// const title = notificationCount + " new notification" + (notificationCount === 1 ? "" : "s");
// let message = '<div class="container"><strong>hello</strong></div>';
//
// message = message + '</div>';
//
// shashin.showToastMessage(title, message, {
//     icon: "bi-bell",
//     iconColor: "#FF8C00",
//     headerSubtext: createdAtDate,
//     autohide: false,
//     borderColor:"warning"
// });




// const myelement = <h1>I Love JSX!</h1>;
//
// ReactDOM.render(myelement, document.getElementById('rnRoot'));