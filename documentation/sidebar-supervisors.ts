import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  supervisorsSidebar: [
    {
      type: 'category',
      label: 'Supervisors & Examiners',
      collapsed: false,
      link: {
        type: 'generated-index',
        description:
          'All information for supervisors and examiners: from posting topics and reviewing applications to running interviews, supervising theses, and issuing grades.',
      },
      items: [
        'overview',
        'supervisor-examiner-guide',
        'permissions',
        'workflows',
        'ai-review-guidelines',
      ],
    },
  ],
};

export default sidebars;
