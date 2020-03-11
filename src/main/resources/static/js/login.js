(function () {
    $(document).ready(function() {
        $("#login").alpaca({
            // "view": "sbs-alpaca-web-edit",
            "view": "web-edit",
            "schema": {
                "type": "object",
                "properties": {
                    "userName": {
                        "type": "string",
                        "title": "Username",
                        "required": true,
                        "pattern": "^[a-zA-Z0-9_]+$"
                    },
                    "password": {
                        "type": "string",
                        "title": "Password",
                        "required": true,
                        "pattern": "^[a-zA-Z0-9_]+$"
                    }
                }
            },
            "options": {
                "renderForm": true,
                "form": {
                    "attributes": {
                        "action": "/api/v1/user/authenticate",
                        "method": "post"
                    },
                    "buttons": {
                        "submit": {}
                    }
                },
                "fields": {
                    "userName": {
                        "size": 20,
                        "label": "Username"
                    },
                    "password": {
                        "type": "password",
                        "size": 20,
                        "label": "Password"
                    }
                }
            },
            "postRender": function(renderedField) {
                var form = renderedField.form;
                if (form) {
                    form.registerSubmitHandler(function(e) {
                        return (renderedField.isValid(true));
                    });
                }
            }
        });
    });
})();
