WITH head_ids AS (SELECT 'fce456ea-66cd-43f5-9ea5-c94fd12d0689'::UUID AS iaas_head_id,
                         '211434d1-1f84-4fba-b8fe-bb5d91eaf448'::UUID AS csbs_head_id)
INSERT
INTO research_groups (research_group_id,
                      head_user_id,
                      name,
                      abbreviation,
                      description,
                      website_url,
                      campus,
                      created_at,
                      created_by,
                      updated_at,
                      updated_by,
                      archived)
SELECT gen_random_uuid(),
       iaas_head_id,
       'Institute for Adaptive Algorithmic Systems',
       'IAAS',
       'The IAAS group conducts interdisciplinary research on adaptive algorithms, self-optimizing systems, and dynamic software infrastructures. Our work bridges theoretical computer science and applied engineering to enable resilient digital platforms.',
       'https://iaas-labs.org/',
       'Munich - Main Campus',
       NOW(),
       iaas_head_id,
       NOW(),
       iaas_head_id,
       FALSE
FROM head_ids
UNION ALL
SELECT gen_random_uuid(),
       csbs_head_id,
       'Center for Scalable Bio-Inspired Systems',
       'CSBS',
       'CSBS focuses on the development of adaptive, nature-inspired computing architectures and algorithmic frameworks that scale across disciplines. Research blends biology, systems theory, and software engineering.',
       'https://csbs-research.org/',
       'Weihenstephan',
       NOW(),
       csbs_head_id,
       NOW(),
       csbs_head_id,
       FALSE
FROM head_ids;