INSERT INTO vehiculos (id_vehiculo, patente, marca, modelo, anio, vin_chasis, id_duenio)
VALUES
('b9c8d7e6-0001-0001-0001-000000000001', 'ABCD12', 'Toyota', 'Corolla', 2020, 'VIN001', 'a1b2c3d4-0001-0001-0001-000000000001'),
('b9c8d7e6-0002-0002-0002-000000000002', 'EFGH34', 'Chevrolet', 'Spark', 2019, 'VIN002', 'a1b2c3d4-0002-0002-0002-000000000002'),
('b9c8d7e6-0003-0003-0003-000000000003', 'IJKL56', 'Hyundai', 'Tucson', 2022, 'VIN003', 'a1b2c3d4-0003-0003-0003-000000000003')
ON CONFLICT (id_vehiculo) DO NOTHING;