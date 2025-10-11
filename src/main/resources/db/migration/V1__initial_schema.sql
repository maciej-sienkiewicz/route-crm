-- Companies table
CREATE TABLE companies (
                           id VARCHAR(50) PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_companies_name ON companies(name);

-- Users table
CREATE TABLE users (
                       id VARCHAR(50) PRIMARY KEY,
                       company_id VARCHAR(50) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       guardian_id VARCHAR(50),
                       driver_id VARCHAR(50),
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                       CONSTRAINT fk_users_company
                           FOREIGN KEY (company_id)
                               REFERENCES companies(id)
                               ON DELETE CASCADE,
                       CONSTRAINT uq_users_company_email UNIQUE (company_id, email)
);

CREATE INDEX idx_users_company ON users(company_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(company_id, role);

-- Guardians table
CREATE TABLE guardians (
                           id VARCHAR(50) PRIMARY KEY,
                           company_id VARCHAR(50) NOT NULL,
                           first_name VARCHAR(255) NOT NULL,
                           last_name VARCHAR(255) NOT NULL,
                           email VARCHAR(255) NOT NULL,
                           phone VARCHAR(20) NOT NULL,
                           alternate_phone VARCHAR(20),
                           address_street VARCHAR(255) NOT NULL,
                           address_house_number VARCHAR(20) NOT NULL,
                           address_apartment_number VARCHAR(20),
                           address_postal_code VARCHAR(10) NOT NULL,
                           address_city VARCHAR(100) NOT NULL,
                           communication_preference VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                           CONSTRAINT fk_guardians_company
                               FOREIGN KEY (company_id)
                                   REFERENCES companies(id)
                                   ON DELETE CASCADE,
                           CONSTRAINT uq_guardians_company_email UNIQUE (company_id, email)
);

CREATE INDEX idx_guardians_company ON guardians(company_id);
CREATE INDEX idx_guardians_company_email ON guardians(company_id, email);
CREATE INDEX idx_guardians_company_name ON guardians(company_id, last_name, first_name);

-- Children table
CREATE TABLE children (
                          id VARCHAR(50) PRIMARY KEY,
                          company_id VARCHAR(50) NOT NULL,
                          first_name VARCHAR(255) NOT NULL,
                          last_name VARCHAR(255) NOT NULL,
                          birth_date DATE NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          disability JSONB NOT NULL,
                          transport_needs JSONB NOT NULL,
                          notes TEXT,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_children_company
                              FOREIGN KEY (company_id)
                                  REFERENCES companies(id)
                                  ON DELETE CASCADE
);

CREATE INDEX idx_children_company ON children(company_id);
CREATE INDEX idx_children_company_status ON children(company_id, status);
CREATE INDEX idx_children_company_name ON children(company_id, last_name, first_name);

-- Guardian assignments (relationship between guardians and children)
CREATE TABLE guardian_assignments (
                                      id VARCHAR(50) PRIMARY KEY,
                                      company_id VARCHAR(50) NOT NULL,
                                      guardian_id VARCHAR(50) NOT NULL,
                                      child_id VARCHAR(50) NOT NULL,
                                      relationship VARCHAR(50) NOT NULL,
                                      is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                      can_pickup BOOLEAN NOT NULL DEFAULT TRUE,
                                      can_authorize BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                      CONSTRAINT fk_assignment_company
                                          FOREIGN KEY (company_id)
                                              REFERENCES companies(id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_assignment_guardian
                                          FOREIGN KEY (guardian_id)
                                              REFERENCES guardians(id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT fk_assignment_child
                                          FOREIGN KEY (child_id)
                                              REFERENCES children(id)
                                              ON DELETE CASCADE,
                                      CONSTRAINT uq_guardian_child UNIQUE (guardian_id, child_id)
);

CREATE INDEX idx_assignments_company ON guardian_assignments(company_id);
CREATE INDEX idx_assignments_guardian ON guardian_assignments(company_id, guardian_id);
CREATE INDEX idx_assignments_child ON guardian_assignments(company_id, child_id);

-- Schedules table
CREATE TABLE schedules (
                           id VARCHAR(50) PRIMARY KEY,
                           company_id VARCHAR(50) NOT NULL,
                           child_id VARCHAR(50) NOT NULL,
                           name VARCHAR(255) NOT NULL,
                           days JSONB NOT NULL,
                           pickup_time TIME NOT NULL,
                           pickup_address_label VARCHAR(100),
                           pickup_address_street VARCHAR(255) NOT NULL,
                           pickup_address_house_number VARCHAR(20) NOT NULL,
                           pickup_address_apartment_number VARCHAR(20),
                           pickup_address_postal_code VARCHAR(10) NOT NULL,
                           pickup_address_city VARCHAR(100) NOT NULL,
                           dropoff_time TIME NOT NULL,
                           dropoff_address_label VARCHAR(100),
                           dropoff_address_street VARCHAR(255) NOT NULL,
                           dropoff_address_house_number VARCHAR(20) NOT NULL,
                           dropoff_address_apartment_number VARCHAR(20),
                           dropoff_address_postal_code VARCHAR(10) NOT NULL,
                           dropoff_address_city VARCHAR(100) NOT NULL,
                           special_instructions TEXT,
                           active BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                           CONSTRAINT fk_schedules_company
                               FOREIGN KEY (company_id)
                                   REFERENCES companies(id)
                                   ON DELETE CASCADE,
                           CONSTRAINT fk_schedules_child
                               FOREIGN KEY (child_id)
                                   REFERENCES children(id)
                                   ON DELETE CASCADE
);

CREATE INDEX idx_schedules_company ON schedules(company_id);
CREATE INDEX idx_schedules_child ON schedules(company_id, child_id);
CREATE INDEX idx_schedules_active ON schedules(company_id, active);

-- Drivers table
CREATE TABLE drivers (
                         id VARCHAR(50) PRIMARY KEY,
                         company_id VARCHAR(50) NOT NULL,
                         first_name VARCHAR(255) NOT NULL,
                         last_name VARCHAR(255) NOT NULL,
                         email VARCHAR(255) NOT NULL,
                         phone VARCHAR(20) NOT NULL,
                         date_of_birth DATE NOT NULL,
                         address_street VARCHAR(255) NOT NULL,
                         address_house_number VARCHAR(20) NOT NULL,
                         address_apartment_number VARCHAR(20),
                         address_postal_code VARCHAR(10) NOT NULL,
                         address_city VARCHAR(100) NOT NULL,
                         license_number VARCHAR(50) NOT NULL,
                         license_categories JSONB NOT NULL,
                         license_valid_until DATE NOT NULL,
                         medical_certificate_valid_until DATE NOT NULL,
                         medical_certificate_issue_date DATE NOT NULL,
                         status VARCHAR(20) NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                         CONSTRAINT fk_drivers_company
                             FOREIGN KEY (company_id)
                                 REFERENCES companies(id)
                                 ON DELETE CASCADE,
                         CONSTRAINT uq_drivers_company_email UNIQUE (company_id, email),
                         CONSTRAINT uq_drivers_company_license UNIQUE (company_id, license_number)
);

CREATE INDEX idx_drivers_company ON drivers(company_id);
CREATE INDEX idx_drivers_company_status ON drivers(company_id, status);

-- Vehicles table
CREATE TABLE vehicles (
                          id VARCHAR(50) PRIMARY KEY,
                          company_id VARCHAR(50) NOT NULL,
                          registration_number VARCHAR(20) NOT NULL,
                          make VARCHAR(100) NOT NULL,
                          model VARCHAR(100) NOT NULL,
                          year INTEGER NOT NULL,
                          vehicle_type VARCHAR(20) NOT NULL,
                          vin VARCHAR(17),
                          capacity_total_seats INTEGER NOT NULL,
                          capacity_wheelchair_spaces INTEGER NOT NULL,
                          capacity_child_seats INTEGER NOT NULL,
                          special_equipment JSONB,
                          insurance_policy_number VARCHAR(100) NOT NULL,
                          insurance_valid_until DATE NOT NULL,
                          insurance_insurer VARCHAR(255) NOT NULL,
                          technical_inspection_valid_until DATE NOT NULL,
                          technical_inspection_station VARCHAR(255) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          current_mileage INTEGER NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_vehicles_company
                              FOREIGN KEY (company_id)
                                  REFERENCES companies(id)
                                  ON DELETE CASCADE,
                          CONSTRAINT uq_vehicles_company_registration UNIQUE (company_id, registration_number)
);

CREATE INDEX idx_vehicles_company ON vehicles(company_id);
CREATE INDEX idx_vehicles_company_status ON vehicles(company_id, status);
CREATE INDEX idx_vehicles_company_type ON vehicles(company_id, vehicle_type);

-- Routes table
CREATE TABLE routes (
                        id VARCHAR(50) PRIMARY KEY,
                        company_id VARCHAR(50) NOT NULL,
                        route_name VARCHAR(255) NOT NULL,
                        date DATE NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        driver_id VARCHAR(50) NOT NULL,
                        vehicle_id VARCHAR(50) NOT NULL,
                        estimated_start_time TIME NOT NULL,
                        estimated_end_time TIME NOT NULL,
                        actual_start_time TIMESTAMP,
                        actual_end_time TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                        CONSTRAINT fk_routes_company
                            FOREIGN KEY (company_id)
                                REFERENCES companies(id)
                                ON DELETE CASCADE,
                        CONSTRAINT fk_routes_driver
                            FOREIGN KEY (driver_id)
                                REFERENCES drivers(id)
                                ON DELETE RESTRICT,
                        CONSTRAINT fk_routes_vehicle
                            FOREIGN KEY (vehicle_id)
                                REFERENCES vehicles(id)
                                ON DELETE RESTRICT
);

CREATE INDEX idx_routes_company ON routes(company_id);
CREATE INDEX idx_routes_company_date ON routes(company_id, date);
CREATE INDEX idx_routes_company_status ON routes(company_id, status);
CREATE INDEX idx_routes_company_driver ON routes(company_id, driver_id);
CREATE INDEX idx_routes_date_status ON routes(date, status);

-- Route children (children assigned to specific routes)
CREATE TABLE route_children (
                                id VARCHAR(50) PRIMARY KEY,
                                company_id VARCHAR(50) NOT NULL,
                                route_id VARCHAR(50) NOT NULL,
                                child_id VARCHAR(50) NOT NULL,
                                schedule_id VARCHAR(50) NOT NULL,
                                pickup_order INTEGER NOT NULL,
                                pickup_address_label VARCHAR(100),
                                pickup_address_street VARCHAR(255) NOT NULL,
                                pickup_address_house_number VARCHAR(20) NOT NULL,
                                pickup_address_apartment_number VARCHAR(20),
                                pickup_address_postal_code VARCHAR(10) NOT NULL,
                                pickup_address_city VARCHAR(100) NOT NULL,
                                dropoff_address_label VARCHAR(100),
                                dropoff_address_street VARCHAR(255) NOT NULL,
                                dropoff_address_house_number VARCHAR(20) NOT NULL,
                                dropoff_address_apartment_number VARCHAR(20),
                                dropoff_address_postal_code VARCHAR(10) NOT NULL,
                                dropoff_address_city VARCHAR(100) NOT NULL,
                                estimated_pickup_time TIME NOT NULL,
                                estimated_dropoff_time TIME NOT NULL,
                                actual_pickup_time TIMESTAMP,
                                actual_dropoff_time TIMESTAMP,
                                status VARCHAR(20) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                CONSTRAINT fk_route_children_company
                                    FOREIGN KEY (company_id)
                                        REFERENCES companies(id)
                                        ON DELETE CASCADE,
                                CONSTRAINT fk_route_children_route
                                    FOREIGN KEY (route_id)
                                        REFERENCES routes(id)
                                        ON DELETE CASCADE,
                                CONSTRAINT fk_route_children_child
                                    FOREIGN KEY (child_id)
                                        REFERENCES children(id)
                                        ON DELETE CASCADE,
                                CONSTRAINT fk_route_children_schedule
                                    FOREIGN KEY (schedule_id)
                                        REFERENCES schedules(id)
                                        ON DELETE RESTRICT,
                                CONSTRAINT uq_route_child UNIQUE (route_id, child_id)
);

CREATE INDEX idx_route_children_company ON route_children(company_id);
CREATE INDEX idx_route_children_route ON route_children(route_id);
CREATE INDEX idx_route_children_child ON route_children(company_id, child_id);
CREATE INDEX idx_route_children_status ON route_children(route_id, status);

-- Route notes
CREATE TABLE route_notes (
                             id VARCHAR(50) PRIMARY KEY,
                             company_id VARCHAR(50) NOT NULL,
                             route_id VARCHAR(50) NOT NULL,
                             author_user_id VARCHAR(50) NOT NULL,
                             author_name VARCHAR(255) NOT NULL,
                             content TEXT NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_route_notes_company
                                 FOREIGN KEY (company_id)
                                     REFERENCES companies(id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_route_notes_route
                                 FOREIGN KEY (route_id)
                                     REFERENCES routes(id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_route_notes_author
                                 FOREIGN KEY (author_user_id)
                                     REFERENCES users(id)
                                     ON DELETE SET NULL
);

CREATE INDEX idx_route_notes_company ON route_notes(company_id);
CREATE INDEX idx_route_notes_route ON route_notes(route_id);
CREATE INDEX idx_route_notes_created ON route_notes(route_id, created_at DESC);

-- Schedule exceptions table
CREATE TABLE schedule_exceptions (
                                     id VARCHAR(50) PRIMARY KEY,
                                     company_id VARCHAR(50) NOT NULL,
                                     schedule_id VARCHAR(50) NOT NULL,
                                     child_id VARCHAR(50) NOT NULL,
                                     exception_date DATE NOT NULL,
                                     notes TEXT,
                                     created_by VARCHAR(50) NOT NULL,
                                     created_by_role VARCHAR(20) NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_exception_company
                                         FOREIGN KEY (company_id)
                                             REFERENCES companies(id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_exception_schedule
                                         FOREIGN KEY (schedule_id)
                                             REFERENCES schedules(id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_exception_child
                                         FOREIGN KEY (child_id)
                                             REFERENCES children(id)
                                             ON DELETE CASCADE,
                                     CONSTRAINT fk_exception_user
                                         FOREIGN KEY (created_by)
                                             REFERENCES users(id)
                                             ON DELETE RESTRICT,
                                     CONSTRAINT uq_exception_schedule_date
                                         UNIQUE (schedule_id, exception_date)
);

CREATE INDEX idx_exceptions_company ON schedule_exceptions(company_id);
CREATE INDEX idx_exceptions_schedule ON schedule_exceptions(company_id, schedule_id);
CREATE INDEX idx_exceptions_child ON schedule_exceptions(company_id, child_id);
CREATE INDEX idx_exceptions_date ON schedule_exceptions(company_id, exception_date);
CREATE INDEX idx_exceptions_child_date ON schedule_exceptions(company_id, child_id, exception_date);