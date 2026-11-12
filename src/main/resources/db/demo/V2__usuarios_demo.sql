-- Usuarios de demostración para probar la API sin crear nada a mano.
--
-- Vive en db/demo y no en db/migration a propósito: en producción basta con
-- FLYWAY_LOCATIONS=classpath:db/migration para que esto nunca se ejecute.
--
-- Contraseña de todos: Demo2026!  (es pública, está en el README; solo sirve
-- para demo). Cada hash se generó con el mismo BCryptPasswordEncoder que usa
-- la app, por eso son distintos aunque la contraseña sea la misma.
--
-- No se crean tickets: un ticket de ejemplo marcado como clasificado por IA
-- sería un dato inventado. Se crean con POST /api/tickets y la IA los clasifica.

INSERT INTO usuarios (nombre, correo, password_hash, rol) VALUES
    ('Carolina Díaz',  'admin@empresa.com',  '$2a$10$v1dS83uheEVxz1hT1tIWku/MMfS.KDnqEbPn5jXeHtiEcIoCILMZa', 'ADMIN'),
    ('Andrés Pérez',   'andres@empresa.com', '$2a$10$LgZ.DLb/PQgZqoaGMEdox.zqmdf4rSHMZLvO5RkMHmbwuKPYyUT7q', 'SOPORTE'),
    ('Marcela Rojas',  'marcela@empresa.com', '$2a$10$P.DDSIjmWsiHHWmA/dRds.zYzh0.Ouf4XplIS0LHHOFsZFKXnhcTW', 'SOPORTE'),
    ('Laura Gómez',    'laura@empresa.com',  '$2a$10$1onGU1rUNXc7GNE0WTBEKOgpbgdEP.xBBpEiwiGFvUeWEzCNoXNr6', 'USUARIO'),
    ('Pedro Ruiz',     'pedro@empresa.com',  '$2a$10$KH6jIbUmDNNaZYzKQxf/pek1XyyoMFfnns/FErwmP.ze9qFD4RcUC', 'USUARIO');
