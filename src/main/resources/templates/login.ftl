<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <title>Relumin</title>

        <!-- css -->
        <link href="/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">
        <link href="/vendor/bootstrap-dark/css/theme.css" rel="stylesheet">
        <style>
        body {
            padding-top: 40px;
            padding-bottom: 40px;
        }
        .form-control {
            color: #555;
        }
        .form-signin {
            max-width: 330px;
            padding: 15px;
            margin: 0 auto;
        }
        .form-signin .form-signin-heading {
            margin-bottom: 40px;
        }
        .form-signin .form-control {
            position: relative;
            height: auto;
            -webkit-box-sizing: border-box;
            -moz-box-sizing: border-box;
            box-sizing: border-box;
            padding: 10px;
            font-size: 16px;
        }
        .form-signin .form-control:focus {
            z-index: 2;
        }
        .form-signin input[type="text"] {
            margin-bottom: -1px;
            border-bottom-right-radius: 0;
            border-bottom-left-radius: 0;
        }
        .form-signin input[type="password"] {
            border-top-left-radius: 0;
            border-top-right-radius: 0;
        }
        .form-signin .checkbox {
            padding-left: 0px;
            margin-bottom: 30px;
        }
        </style>
    </head>
    <body>
        <div class="container">
            <form class="form-signin" action="/login" method="post">
                <h2 class="form-signin-heading">
                    Relumin
                </h2>
                <label>Input ID/Password</label>
                <#if RequestParameters.error??>
                    <div class="alert alert-danger" style="padding: 5px;">
                        <span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span> Incorrect ID or password
                    </div>
                </#if>
                <input type="text" name="username" class="form-control" placeholder="Login ID" required autofocus>
                <input type="password" name="password" class="form-control" placeholder="Password" required>
                <div class="checkbox">
                    <label>
                        <input type="checkbox" name="remember-me"> Remember me
                    </label>
                </div>
                <button class="btn btn-primary btn-block" type="submit">Login</button>
            </form>
        </div>
    </body>
</html>
