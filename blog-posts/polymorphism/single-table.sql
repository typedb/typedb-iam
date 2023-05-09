CREATE TABLE employee (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(256) NOT NULL,
    last_name VARCHAR(256),
    employment_type VARCHAR(256),
    start_date DATE,
    end_date DATE,
    agency_id INT REFERENCES agency(id),
    salary MONEY,
    hourly_rate MONEY
);



CREATE TABLE unit_membership (
    business_unit_id INT REFERENCES business_unit(id) NOT NULL,
    member_employee_id INT REFERENCES employee(id) DEFAULT NULL,
    member_unit_id INT REFERENCES business_unit(id) DEFAULT NULL
);