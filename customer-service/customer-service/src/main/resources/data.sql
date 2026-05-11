INSERT INTO clientes (id_cliente, nombre, apellido, email, telefono, direccion)
VALUES
('a1b2c3d4-0001-0001-0001-000000000001', 'Juan', 'Pérez', 'juan.perez@mail.com', '+56912345678', 'Av. Siempre Viva 123'),
('a1b2c3d4-0002-0002-0002-000000000002', 'María', 'González', 'maria.gonzalez@mail.com', '+56987654321', 'Calle Falsa 456'),
('a1b2c3d4-0003-0003-0003-000000000003', 'Carlos', 'Rodríguez', 'carlos.rodriguez@mail.com', '+56911111111', 'Los Aromos 789')
ON CONFLICT (id_cliente) DO NOTHING;