$(document).ready(function() {
    $('[data-toggle="tooltip"]').tooltip();
    $('[data-toggle="popover"]').popover();

    $('#keyboard').keyboard({
        theme: 'monokai',
        is_hidden: true,
        close_speed: 500,
        trigger: undefined
    });
});