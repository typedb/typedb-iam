CREATE TABLE employee (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(256) NOT NULL,
    last_name VARCHAR(256) NOT NULL
);

CREATE TABLE permanent_employee (
    id INT PRIMARY KEY REFERENCES employee(id),
    start_date DATE NOT NULL,
    end_date DATE DEFAULT NULL,
    salary MONEY NOT NULL
);

CREATE TABLE contractor (
    id INT PRIMARY KEY REFERENCES employee(id),
    agency_id INT REFERENCES agency(id) NOT NULL,
    hourly_rate MONEY NOT NULL
);



CREATE TABLE unit_membership (
    business_unit_id INT REFERENCES business_unit(id),
    member_id INT REFERENCES subject(id),
    PRIMARY KEY (business_unit_id, member_id)
);

CREATE TABLE subject (
    id SERIAL PRIMARY KEY
);

CREATE TABLE employee (
    id INT PRIMARY KEY REFERENCES subject(id),
    ...
);

CREATE TABLE business_unit (
    id INT PRIMARY KEY REFERENCES subject(id),
    ...
);