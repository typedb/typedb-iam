CREATE TABLE subject (
    id SERIAL PRIMARY KEY
);

CREATE TABLE users (
    id INT PRIMARY KEY REFERENCES subject(id)
);

CREATE TABLE user_group (
    id INT PRIMARY KEY REFERENCES subject(id)
);

CREATE TABLE object (
    id SERIAL PRIMARY KEY
);

CREATE TABLE object_type (
    object INT REFERENCES object(id),
    object_type VARCHAR(256),
    PRIMARY KEY(object, object_type)
);

CREATE TABLE resource (
    id INT PRIMARY KEY REFERENCES object(id)
);

CREATE TABLE resource_collection (
    id INT PRIMARY KEY REFERENCES object(id)
);

CREATE TABLE actions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE action_type (
    action_id INT REFERENCES actions(id),
    object_type VARCHAR(256),
    PRIMARY KEY (action_id, object_type)
);

CREATE TABLE operation (
    id INT PRIMARY KEY REFERENCES actions(id)
);

CREATE TABLE operation_set (
    id INT PRIMARY KEY REFERENCES actions(id)
);

CREATE TABLE group_membership (
    user_group INT REFERENCES user_group(id),
    group_member INT REFERENCES subject(id),
    PRIMARY KEY(user_group, group_member)
);

CREATE TABLE collection_membership (
    resource_collection INT REFERENCES resource_collection(id),
    collection_member INT REFERENCES object(id),
    PRIMARY KEY(resource_collection, collection_member)
);

CREATE TABLE set_membership (
    operation_set INT REFERENCES operation_set(id),
    set_member INT REFERENCES actions(id),
    PRIMARY KEY(operation_set, set_member)
);

CREATE TABLE group_ownership (
    owned_group INT REFERENCES user_group(id),
    group_owner INT REFERENCES subject(id),
    ownership_type VARCHAR(256)
    PRIMARY KEY(owned_group, ownership_type)
);

CREATE TABLE object_ownership (
    owned_object INT REFERENCES object(id),
    object_owner INT REFERENCES subject(id),
    ownership_type VARCHAR(256),
    PRIMARY KEY(owned_object, ownership_type)
);

CREATE TABLE access (
    id SERIAL NOT NULL UNIQUE,
    accessed_object INT REFERENCES object(id),
    valid_action INT REFERENCES actions(id),
    PRIMARY KEY(accessed_object, valid_action)
);

CREATE TABLE permission (
    permitted_subject INT REFERENCES subject(id),
    permitted_access INT REFERENCES access(id),
    review_date DATE,
    validity BOOLEAN,
    PRIMARY KEY(permitted_subject, permitted_access)
);

CREATE TABLE change_request (
    requesting_subject INT REFERENCES subject(id),
    requested_subject INT REFERENCES subject(id),
    requested_change INT REFERENCES access(id)
);

CREATE TABLE segregation_policy (
    id SERIAL NOT NULL UNIQUE,
    name VARCHAR(256) NOT NULL UNIQUE,
    segregated_action_1 INT REFERENCES actions(id),
    segregated_action_2 INT REFERENCES actions(id),
    PRIMARY KEY(segregated_action_1, segregated_action_2)
);

CREATE TABLE segregation_violation (
    violating_subject INT REFERENCES subject(id),
    violating_object INT REFERENCES object(id),
    violated_policy INT REFERENCES segregation_policy(id),
    PRIMARY KEY (violating_subject, violating_object, violated_policy)
);

CREATE TABLE person (
    id INT PRIMARY KEY REFERENCES users(id),
    name VARCHAR(256),
    email VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE business_unit (
    id INT PRIMARY KEY REFERENCES user_group(id),
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE user_role (
    id INT PRIMARY KEY REFERENCES user_group(id),
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE user_account (
    id INT PRIMARY KEY REFERENCES user_group(id),
    email VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE file (
    id INT PRIMARY KEY REFERENCES resource(id),
    filepath VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE interface (
    id INT PRIMARY KEY REFERENCES resource(id),
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE records (
    id INT PRIMARY KEY REFERENCES resource(id),
    number INT NOT NULL UNIQUE
);

CREATE TABLE directory (
    id INT PRIMARY KEY REFERENCES resource_collection(id),
    filepath VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE application (
    id INT PRIMARY KEY REFERENCES resource_collection(id),
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE databases (
    id INT PRIMARY KEY REFERENCES resource_collection(id),
    name VARCHAR(256) NOT NULL UNIQUE
);

CREATE TABLE tables (
    id INT PRIMARY KEY REFERENCES resource_collection(id),
    name VARCHAR(256) NOT NULL UNIQUE
);