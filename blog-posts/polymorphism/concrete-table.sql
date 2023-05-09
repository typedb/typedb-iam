CREATE TABLE permanent_employee (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(256),
    last_name VARCHAR(256),
    start_date DATE NOT NULL,
    end_date DATE DEFAULT NULL,
    salary MONEY NOT NULL
);

CREATE TABLE contractor (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(256) NOT NULL,
    last_name VARCHAR(256) NOT NULL,
    agency_id INT REFERENCES agency(id) NOT NULL,
    hourly_rate MONEY NOT NULL
);



CREATE TABLE unit_membership (
    business_unit_id INT REFERENCES business_unit(id),
    member_table VARCHAR(256),
    member_id INT,
    PRIMARY KEY (business_unit_id, member_table, member_id)
);