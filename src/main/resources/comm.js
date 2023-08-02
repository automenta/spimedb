//<script src='https://meet.jit.si/external_api.js'></script>
function comm(target) {
    const e = $('<div>')
        .attr('id', 'comm')
        .css('position', 'fixed').css('width', '700px').css('height', '700px');

    target.append(e);

    setTimeout(()=>{
        const parentNode = document.querySelector('#comm') //HACK;

        const domain = 'meet.jit.si';
        const options = {
            roomName: 'netjsvr',
            width: '100%',  height: '100%',
            parentNode: parentNode
        };

        const jitsi = new JitsiMeetExternalAPI(domain, options);
        jitsi.executeCommand('toggleChat');
    });
}