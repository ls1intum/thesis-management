import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'Thesis Management',
  tagline: 'Documentation for Users',
  favicon: 'img/favicon.svg',

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://docs.thesis.aet.cit.tum.de',
  // Set the /<baseUrl>/ pathname under which your site is served
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'ls1intum', // Usually your GitHub org/user name.
  projectName: 'thesis-management', // Usually your repo name.

  onBrokenLinks: 'throw',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  presets: [
    [
      'classic',
      {
        docs: false,
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'students',
        path: 'docs/students',
        routeBasePath: 'students',
        sidebarPath: './sidebar-students.ts',
        editUrl:
          'https://github.com/ls1intum/thesis-management/tree/develop/documentation',
      },
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'supervisors',
        path: 'docs/supervisors-and-examiners',
        routeBasePath: 'supervisors',
        sidebarPath: './sidebar-supervisors.ts',
        editUrl:
          'https://github.com/ls1intum/thesis-management/tree/develop/documentation',
      },
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'developer',
        path: 'docs/developer',
        routeBasePath: 'developer',
        sidebarPath: './sidebar-developer.ts',
        editUrl:
          'https://github.com/ls1intum/thesis-management/tree/develop/documentation',
      },
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'admin',
        path: 'docs/admins',
        routeBasePath: 'admin',
        sidebarPath: './sidebar-admin.ts',
        editUrl:
          'https://github.com/ls1intum/thesis-management/tree/develop/documentation',
      },
    ],
    [
      require.resolve('@easyops-cn/docusaurus-search-local'),
      {
        hashed: true,
        indexDocs: true,
        indexBlog: false,
        docsRouteBasePath: ['students', 'supervisors', 'developer', 'admin'],
        docsPluginIdForPreferredVersion: 'students',
        searchContextByPaths: ['students', 'supervisors', 'developer', 'admin'],
        hideSearchBarWithNoSearchContext: true,
      },
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Thesis Management Docs',
      logo: {
        alt: 'Thesis Management Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          to: '/students/',
          label: 'Students',
          position: 'left',
        },
        {
          to: '/supervisors/',
          label: 'Supervisors & Examiners',
          position: 'left',
        },
        {
          to: '/developer/',
          label: 'Developers',
          position: 'left',
        },
        {
          to: '/admin/',
          label: 'Admins',
          position: 'left',
        },
        {
          href: 'https://github.com/ls1intum/thesis-management',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Community',
          items: [
            {
              label: 'AET Website',
              href: 'https://aet.cit.tum.de',
            },
            {
              label: 'AET LinkedIn',
              href: 'https://www.linkedin.com/company/tumaet',
            },
            {
              label: 'AET Instagram',
              href: 'https://www.instagram.com/tum.aet',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub - Thesis Management',
              href: 'https://github.com/ls1intum/thesis-management',
            },
            {
              label: 'GitHub - AET Projects',
              href: 'https://github.com/ls1intum',
            },
          ],
        },
        {
          title: 'Legal',
          items: [
            {
              label: 'Imprint',
              to: '/imprint',
            },
            {
              label: 'About Us',
              to: '/about',
            },
          ],
        },
      ],
      copyright: `© ${new Date().getFullYear()} TUM Applied Education Technologies`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
