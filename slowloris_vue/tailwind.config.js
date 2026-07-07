module.exports = {
    content: [
        './index.html',
        './src/**/*.{vue,js,ts,jsx,tsx}',
    ],
    theme: {
        extend: {
            colors: {
                brand: 'var(--color-brand)',
                surface: {
                    base: 'var(--color-surface-base)',
                    raised: 'var(--color-surface-raised)',
                    card: 'var(--color-surface-card)',
                    light: 'var(--color-surface-light)',
                },
                threat: {
                    critical: 'var(--color-threat-critical)',
                    high: 'var(--color-threat-high)',
                    medium: 'var(--color-threat-medium)',
                    low: 'var(--color-threat-low)',
                    info: 'var(--color-threat-info)',
                },
                // keep legacy tokens
                primary: '#3b82f6',
                secondary: '#36CFC9',
                danger: '#ef4444',
                warning: '#f59e0b',
                success: '#22c55e',
                info: '#06b6d4',
                dark: '#0f172a',
                light: '#f8fafc',
            },
            fontFamily: {
                sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
                inter: ['Inter', 'sans-serif'],
                mono: ['JetBrains Mono', 'Fira Code', 'ui-monospace', 'monospace'],
            },
            boxShadow: {
                'card': 'var(--shadow-card)',
                'card-hover': 'var(--shadow-card-hover)',
                'sidebar': 'var(--shadow-sidebar)',
                'glow-brand': '0 0 20px rgba(59, 130, 246, 0.25)',
                'glow-threat': '0 0 20px rgba(239, 68, 68, 0.25)',
            },
            borderRadius: {
                'card': '0.75rem',
            },
        },
    },
    plugins: [],
}
