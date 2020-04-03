var userName = document.getElementById("userName")
var firstName = document.getElementById("firstName")
var lastName = document.getElementById("lastName")
var dateOfBirth = document.getElementById("dateOfBirth")
var phoneNumber = document.getElementById("phoneNumber")
var email = document.getElementById("email")
var ssn = document.getElementById("ssn")
var save = document.getElementById("save")
var profileBtn = document.getElementById("profileBtn")

function modify(x) {
    if (x.disabled) {
        x.disabled = false;
    } else {
        x.disabled = true;
    }
}

function editProfile() {
    modify(phoneNumber);
    modify(email);
    if (save.disabled) {
        save.disabled = false;
    }
    profileBtn.disabled = true;
}

function editAll() {
    modify(userName);
    modify(firstName);
    modify(lastName);
    modify(dateOfBirth);
    modify(ssn);
}

