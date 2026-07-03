import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  developerSidebar: [
    {
      type: 'category',
      label: 'Developers',
      collapsed: false,
      link: {
        type: 'generated-index',
        description:
          'All information for developers: from setting up your local environment to database changes, mail templates, releases, and the API reference.',
      },
      items: [
        'overview',
        'development-setup',
        'database',
        'mails',
        'release-workflow',
        'api-reference',
      ],
    },
  ],
};

export default sidebars;
