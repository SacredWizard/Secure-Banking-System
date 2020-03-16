var len=document.getElementById("len");
var password=document.getElementById("password");
var confirmPassword=document.getElementById("confirmPassword");

password.onfocus = function() {
    document.getElementById("message").style.display = "block";
}
password.onblur = function() {
    document.getElementById("message").style.display = "none";
}

function validatePassword() {
    if (password.value.length >= 4) {
        len.classList.remove("invalid");
        len.classList.add("valid");
    } else {
        len.classList.remove("valid");
        len.classList.add("invalid");
    }

    if(password.value != confirmPassword.value) {
        confirmPassword.setCustomValidity("Passwords Don't Match");
    } else {
        confirmPassword.setCustomValidity('');
    }
}
password.onchange = validatePassword;
confirmPassword.onkeyup = validatePassword;