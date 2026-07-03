import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  studentsSidebar: [
    {
      type: 'category',
      label: 'Students',
      collapsed: false,
      link: {
        type: 'generated-index',
        description:
          'All information for students: from creating an account and browsing topics to applying, interviewing, writing your thesis, and receiving your grade.',
      },
      items: [
        'overview',
        'student-guide',
      ],
    },
  ],
};

export default sidebars;
