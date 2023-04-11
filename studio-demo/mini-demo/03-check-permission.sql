WITH target_subject AS (
    SELECT subject.id AS subject_id
    FROM subject
    INNER JOIN person USING (id)
    WHERE person.email = 'douglas.schmidt@vaticle.com'
)
WITH target_object AS (
    SELECT object.id AS object_id
    FROM object
    INNER JOIN file USING (id)
    WHERE file.path = 'root/engineering/typedb-studio/src/README.md'
)
WITH target_action AS (
    SELECT action.id AS action_id
    FROM action
    INNER JOIN operation USING (id)
    WHERE operation.name = 'edit file'
)
WITH RECURSIVE parent_group AS (
    SELECT group_id
    FROM group_membership
    WHERE member_id IN (
        SELECT subject_id
        FROM target_subject
    )
    UNION ALL
    SELECT group_membership.group_id
    FROM group_membership
    INNER JOIN parent_group
    ON parent_group.group_id = group_membership.member_id
)
WITH RECURSIVE parent_collection AS (
    SELECT collection_id
    FROM collection_membership
    WHERE member_id IN (
        SELECT object_id
        FROM target_object
    )
    UNION ALL
    SELECT collection_membership.collection_id
    FROM collection_membership
    INNER JOIN parent_collection
    ON parent_collection.collection_id = collection_membership.member_id
)
WITH RECURSIVE parent_set AS (
    SELECT set_id
    FROM set_membership
    WHERE member_id IN (
        SELECT action_id
        FROM target_action
    )
    UNION ALL
    SELECT set_membership.set_id
    FROM set_membership
    INNER JOIN parent_set
    ON parent_set.set_id = set_membership.member_id
)
WITH candidate_access AS (
    SELECT id AS access_id
    FROM access
    WHERE object_id IN (
        SELECT object_id
        FROM target_object
        UNION ALL
        SELECT collection_id
        FROM parent_collection
    )
    AND action_id IN (
        SELECT action_id
        FROM target_action
        UNION ALL
        SELECT set_id
        FROM parent_set
    )
)
SELECT EXISTS (
    SELECT *
    FROM permission
    WHERE subject_id IN (
        SELECT subject_id
        FROM target_subject
        UNION ALL
        SELECT group_id
        FROM parent_group
    )
    AND access_id IN (
        SELECT access_id
        FROM candidate_access
    )
);
