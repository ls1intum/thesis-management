import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  adminSidebar: [
    {
      type: 'category',
      label: 'Admins',
      collapsed: false,
      link: {
        type: 'generated-index',
        description:
          'All information for admins: platform configuration, production deployment, and data retention.',
      },
      items: [
        'overview',
        'configuration',
        'production-setup',
        'data-retention',
      ],
    },
  ],
};

export default sidebars;
