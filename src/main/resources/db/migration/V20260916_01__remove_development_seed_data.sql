-- Remove the demo business records that were historically shipped in production migrations.
-- The fixed IDs keep this cleanup isolated from records created through the application.

CREATE TEMPORARY TABLE cleanup_development_house_ids (
    id VARCHAR(36) PRIMARY KEY
);

INSERT INTO cleanup_development_house_ids (id) VALUES
    ('house-1'), ('house-2'), ('house-3'), ('house-4'), ('house-5'), ('house-6'),
    ('house-7'), ('house-8'), ('house-9'), ('house-10'), ('house-11'), ('house-12'),
    ('house-13'), ('house-14'), ('house-15'), ('house-16'), ('house-17'), ('house-18');

-- Remove transactional and derived records first so local/test databases do not retain orphans.
DELETE target FROM appointment_access_grant target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM appointment target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM conversation target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM deposit_record target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_facility_relation target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_image target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_inspection_template target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_location target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_property_certificate target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_recommendation_stats target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_rental_reservation target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_tag_relation target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM house_viewing_config target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM immersive_tour target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM lease_inspection_snapshot target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM lease_termination_applications target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM `lease` target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM lock_device target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM lock_permission target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM payment_record target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM recommendation_event target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM rent_contract target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM rent_order target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM repair_record target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM smart_locks target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;
DELETE target FROM user_favorite_house target
INNER JOIN cleanup_development_house_ids seed ON seed.id = target.house_id;

DELETE advertisement
FROM advertisement
INNER JOIN cleanup_development_house_ids seed
        ON advertisement.target_type = 'house'
       AND advertisement.target_value = seed.id;

DELETE house
FROM house
INNER JOIN cleanup_development_house_ids seed ON seed.id = house.id;

-- The two historical demo landlords were converted into inactive users by later migrations.
DELETE FROM landlord WHERE id IN ('landlord-1', 'landlord-2');
DELETE demo_user
FROM `user` demo_user
LEFT JOIN house ON house.landlord_id = demo_user.id
LEFT JOIN landlord ON landlord.user_id = demo_user.id
WHERE demo_user.id IN ('landlord-1', 'landlord-2')
  AND house.id IS NULL
  AND landlord.id IS NULL;

-- Remove fake communities and the incomplete region sample only when no real record uses them.
DELETE demo_community
FROM community demo_community
LEFT JOIN house ON house.community_id = demo_community.id
WHERE demo_community.id IN ('community-1', 'community-2', 'community-3')
  AND house.id IS NULL;

DELETE demo_region
FROM region demo_region
LEFT JOIN community ON community.region_id = demo_region.id
WHERE demo_region.id IN ('region-yubei', 'region-jiangbei', 'region-yuzhong')
  AND community.id IS NULL;

DROP TEMPORARY TABLE cleanup_development_house_ids;
