-- Provinces of Angola. Reflects the 2024 administrative reorganization
-- (Cuando-Cubango → Cuando + Cubango, Moxico → Moxico + Moxico Leste, Luanda → Luanda + Icolo e Bengo).
CREATE TABLE provinces (
    code VARCHAR(3)  PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO provinces (code, name) VALUES
    ('BGO', 'Bengo'),
    ('BGL', 'Benguela'),
    ('BIE', 'Bié'),
    ('CAB', 'Cabinda'),
    ('CDO', 'Cuando'),
    ('CBG', 'Cubango'),
    ('CNO', 'Cuanza Norte'),
    ('CSL', 'Cuanza Sul'),
    ('CUN', 'Cunene'),
    ('HUA', 'Huambo'),
    ('HUI', 'Huíla'),
    ('IBE', 'Icolo e Bengo'),
    ('LUA', 'Luanda'),
    ('LNO', 'Lunda Norte'),
    ('LSL', 'Lunda Sul'),
    ('MAL', 'Malanje'),
    ('MOX', 'Moxico'),
    ('MXL', 'Moxico Leste'),
    ('NAM', 'Namibe'),
    ('UIG', 'Uíge'),
    ('ZAI', 'Zaire');

CREATE TABLE municipios (
    id             UUID         PRIMARY KEY,
    provincia_code VARCHAR(3)   NOT NULL REFERENCES provinces (code),
    name           VARCHAR(64)  NOT NULL,
    CONSTRAINT uk_municipios_prov_name UNIQUE (provincia_code, name)
);

CREATE INDEX idx_municipios_provincia ON municipios (provincia_code);

-- Provincial capitals and major municipalities. Not exhaustive — schools can request additions.
-- IDs are deterministic v3 UUIDs derived from the "prov:muni" string in a companion migration,
-- but for seeding purposes we use hardcoded stable UUIDs.

INSERT INTO municipios (id, provincia_code, name) VALUES
    -- Luanda (6 municípios after Icolo e Bengo split)
    ('a1111111-0000-0000-0000-000000000001', 'LUA', 'Belas'),
    ('a1111111-0000-0000-0000-000000000002', 'LUA', 'Cacuaco'),
    ('a1111111-0000-0000-0000-000000000003', 'LUA', 'Cazenga'),
    ('a1111111-0000-0000-0000-000000000004', 'LUA', 'Luanda'),
    ('a1111111-0000-0000-0000-000000000005', 'LUA', 'Talatona'),
    ('a1111111-0000-0000-0000-000000000006', 'LUA', 'Viana'),
    -- Icolo e Bengo (moved from Luanda in 2024)
    ('a1112000-0000-0000-0000-000000000001', 'IBE', 'Ícolo e Bengo'),
    ('a1112000-0000-0000-0000-000000000002', 'IBE', 'Quiçama'),
    -- Benguela — top municípios
    ('a2222222-0000-0000-0000-000000000001', 'BGL', 'Benguela'),
    ('a2222222-0000-0000-0000-000000000002', 'BGL', 'Lobito'),
    ('a2222222-0000-0000-0000-000000000003', 'BGL', 'Catumbela'),
    -- Provincial capitals for the rest (add more municípios per province in a later migration)
    ('a3300000-0000-0000-0000-000000000001', 'BGO', 'Caxito'),
    ('a3300000-0000-0000-0000-000000000002', 'BIE', 'Kuito'),
    ('a3300000-0000-0000-0000-000000000003', 'CAB', 'Cabinda'),
    ('a3300000-0000-0000-0000-000000000004', 'CDO', 'Mavinga'),
    ('a3300000-0000-0000-0000-000000000005', 'CBG', 'Menongue'),
    ('a3300000-0000-0000-0000-000000000006', 'CNO', 'Ndalatando'),
    ('a3300000-0000-0000-0000-000000000007', 'CSL', 'Sumbe'),
    ('a3300000-0000-0000-0000-000000000008', 'CUN', 'Ondjiva'),
    ('a3300000-0000-0000-0000-000000000009', 'HUA', 'Huambo'),
    ('a3300000-0000-0000-0000-00000000000a', 'HUI', 'Lubango'),
    ('a3300000-0000-0000-0000-00000000000b', 'LNO', 'Dundo'),
    ('a3300000-0000-0000-0000-00000000000c', 'LSL', 'Saurimo'),
    ('a3300000-0000-0000-0000-00000000000d', 'MAL', 'Malanje'),
    ('a3300000-0000-0000-0000-00000000000e', 'MOX', 'Luena'),
    ('a3300000-0000-0000-0000-00000000000f', 'MXL', 'Cazombo'),
    ('a3300000-0000-0000-0000-000000000010', 'NAM', 'Moçâmedes'),
    ('a3300000-0000-0000-0000-000000000011', 'UIG', 'Uíge'),
    ('a3300000-0000-0000-0000-000000000012', 'ZAI', 'Mbanza Kongo');

CREATE TABLE schools (
    id                  UUID         PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    code                VARCHAR(32)  NOT NULL UNIQUE,
    municipio_id        UUID         NOT NULL REFERENCES municipios (id),
    comuna_ou_bairro    VARCHAR(128),
    address_line1       VARCHAR(255),
    address_complement  VARCHAR(255),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schools_municipio ON schools (municipio_id);

-- Demo tenant. School.id is the tenant_id used by every other module.
INSERT INTO schools (id, name, code, municipio_id, comuna_ou_bairro, address_line1)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Escola Demo Skool',
    'DEMO',
    'a1111111-0000-0000-0000-000000000004', -- Luanda / Luanda
    'Ingombota',
    'Rua da Missão'
);
