(function () {
    Alpaca.registerView({
        "id": "sbs-alpaca-web-edit",
        "parent": "web-edit",
        "callbacks": {
            "form": function () {
                let form = $(this.form);
                form.addClass("form-group");
                form.find("button[type=submit]").addClass("btn btn-primary");
                form.find("button[type=button]").addClass("btn btn-secondary");
            },
            "field": function () {
                $(this.getFieldEl()).addClass("form-control");
            },
            "required": function () {
                $(this.getFieldEl()).addClass("form-control");
            }
        }
    });
})();