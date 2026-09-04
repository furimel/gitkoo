import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'GitKoo',
  description: 'A self-hosted Git forge - simple, fast, yours.',
  lang: 'en-US',
  cleanUrls: true,
  lastUpdated: true,

  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Architecture', link: '/architecture' },
      { text: 'Workflow DSL', link: '/workflow-dsl' },
      { text: 'GitHub', link: 'https://github.com/furimeo/gitkoo' },
    ],

    sidebar: [
      {
        text: 'Overview',
        items: [
          { text: 'Architecture', link: '/architecture' },
          { text: 'Stack', link: '/stack' },
          { text: 'Testing', link: '/testing' },
        ],
      },
      {
        text: 'Core',
        items: [
          { text: 'Storage', link: '/storage' },
          { text: 'Database Schema', link: '/database-schema' },
          { text: 'Authentication', link: '/auth' },
          { text: 'Permissions', link: '/permissions' },
          { text: 'Git Transport', link: '/git-transport' },
        ],
      },
      {
        text: 'Workflow',
        items: [
          { text: 'Workflow DSL', link: '/workflow-dsl' },
        ],
      },
      {
        text: 'Other',
        items: [
          { text: 'Security', link: '/security' },
          { text: 'UI Design', link: '/ui' },
        ],
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/furimeo/gitkoo' },
    ],

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright (c) 2026 furimeo',
    },

    outline: {
      level: [2, 3],
    },

    search: {
      provider: 'local',
    },
  },
})
