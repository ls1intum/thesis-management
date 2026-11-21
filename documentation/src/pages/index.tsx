import type { ReactNode } from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import styles from './index.module.css';

const sections = [
  {
    title: 'Students',
    icon: '🎓',
    description: 'Browse topics, apply, interview, and complete your thesis.',
    link: '/students/',
  },
  {
    title: 'Supervisors & Examiners',
    icon: '👩‍🏫',
    description: 'Post topics, review applications, run interviews, and grade.',
    link: '/supervisors/',
  },
  {
    title: 'Developers',
    icon: '💻',
    description: 'Setup, database, mails, releases, and API reference.',
    link: '/developer/',
  },
  {
    title: 'Admins',
    icon: '🔧',
    description: 'Configuration, production setup, and data retention.',
    link: '/admin/',
  },
];

function Tile({title, icon, description, link}: (typeof sections)[number]) {
  return (
    <Link to={link} className={styles.tile}>
      <div className={styles.tileIcon}>{icon}</div>
      <h3 className={styles.tileTitle}>{title}</h3>
      <p className={styles.tileDescription}>{description}</p>
    </Link>
  );
}

function HomepageHeader() {
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          Welcome to the Thesis Management Documentation
        </Heading>
        <p className="hero__subtitle">
          The official guide for Students, Supervisors, Examiners, Developers,
          and Admins using the Thesis Management platform.
        </p>
        <div className={styles.tileGrid}>
          {sections.map((section) => (
            <Tile
              key={section.title}
              title={section.title}
              icon={section.icon}
              description={section.description}
              link={section.link}
            />
          ))}
        </div>
      </div>
    </header>
  );
}

export default function Home(): ReactNode {
  return (
    <Layout
      title={`Thesis Management Documentation`}
      description="Official documentation for the TUM Thesis Management platform."
    >
      <HomepageHeader />
    </Layout>
  );
}
