-- Create 'speciality' table if it doesn't exist
CREATE TABLE IF NOT EXISTS speciality
(
    id          INT          NOT NULL AUTO_INCREMENT,
    description VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Create 'doctor' table if it doesn't exist
CREATE TABLE IF NOT EXISTS doctor
(
    id            INT          NOT NULL AUTO_INCREMENT,
    speciality_id INT          NOT NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    title         VARCHAR(45)  NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(30)  NOT NULL,
    department    VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_doctor_1
        FOREIGN KEY (speciality_id)
            REFERENCES speciality (id)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
);

CREATE INDEX idx_doctor_1 ON doctor (speciality_id);
CREATE INDEX idx_doctor_2 ON doctor (last_name);
CREATE INDEX idx_doctor_3 ON doctor (email);
CREATE INDEX idx_doctor_4 ON doctor (department);

-- Create 'patient' table if it doesn't exist
CREATE TABLE IF NOT EXISTS patient
(
    id            INT          NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    date_of_birth DATE         NOT NULL,
    address       VARCHAR(150) DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_patient_1 ON patient (first_name);
CREATE INDEX idx_patient_2 ON patient (email);
CREATE INDEX idx_patient_3 ON patient (last_name);

-- Create 'appointment' table if it doesn't exist
CREATE TABLE IF NOT EXISTS appointment
(
    id                  INT          NOT NULL AUTO_INCREMENT,
    patient_id          INT          NOT NULL,
    doctor_id           INT          NOT NULL,
    start_time          TIMESTAMP    NOT NULL,
    end_time            TIMESTAMP    NOT NULL,
    duration            INT          NOT NULL,
    title               VARCHAR(100) NOT NULL,
    description         TEXT,
    notes               VARCHAR(100) DEFAULT NULL,
    follow_up_required  BOOLEAN      DEFAULT NULL,
    cancellation_time   TIMESTAMP    DEFAULT NULL,
    cancellation_reason VARCHAR(100) DEFAULT NULL,
    type                VARCHAR(50)  NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL,
    last_updated        TIMESTAMP    DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointment_1
        FOREIGN KEY (doctor_id)
            REFERENCES doctor (id)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
    CONSTRAINT fk_appointment_2
        FOREIGN KEY (patient_id)
            REFERENCES patient (id)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
);

CREATE INDEX idx_appointment_1 ON appointment (patient_id);
CREATE INDEX idx_appointment_2 ON appointment (start_time);
CREATE INDEX idx_appointment_3 ON appointment (status);
CREATE INDEX idx_appointment_4 ON appointment (doctor_id);