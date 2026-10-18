UPDATE users
SET email = 'ritesh@gmail.com',
    password_hash = '201e81408f0c53f978733167d7de65e0cc4d717ef0db6814ad19c637a2626850',
    salt = 'somesaltforadmin'
WHERE role = 'admin';
