<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Yaşar Bilgi Login</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body>
<div class="login-container">
    <img src="${url.resourcesPath}/img/yasarstaj.png" alt="Logo" class="logo"/>

    <div class="form-container">
        <h2>Yaşar Bilgi Çalışma Günleri Kayıt Sistemi</h2>

        <form method="post" action="${url.loginAction}">
            <div class="input-group">
                <span class="icon">👤</span>
                <input type="text" name="username" placeholder="Kullanıcı Adı" required>
            </div>

            <div class="input-group">
                <span class="icon">🔑</span>
                <input type="password" name="password" placeholder="Şifre" required>
            </div>

            <button type="submit" class="login-button">Giriş</button>
        </form>
    </div>
</div>
</body>
</html>