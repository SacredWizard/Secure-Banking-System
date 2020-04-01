$('.selectField').hide();
$(function() {
    $('#mode').change(function(){
        $('.selectField').hide();
        $('.inputFields').prop( "disabled", true );
        $('.' + $(this).val()).show();
        $('.' + $(this).val()).prop( "disabled", false );
    });
});