const mobileMenuToggle = document.getElementById('mobileMenuToggle');
const mainNav = document.getElementById('mainNav');
const secondaryNav = document.getElementById('secondaryNav');

// Navigation data structure
const navData = {
    home: null,
    download: null,
    about: ['Overview', 'Practice', 'Online', 'Analysis', 'Poker Clock', 'Competition', 'Screenshots', 'FAQs'],
    store: ['Products', 'Pricing', 'License', 'Upgrades'],
    forums: ['General', 'Strategy', 'Technical Support', 'Bug Reports'],
    support: ['Getting Started', 'Documentation', 'Troubleshooting', 'Contact'],
    online: ['Game Portal', 'Leaderboards', 'Tournaments', 'Player Stats']
};

// Mobile menu toggle
mobileMenuToggle.addEventListener('click', () => {
    mainNav.classList.toggle('open');
});

// Main navigation click handling
document.querySelectorAll('.main-nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();

        const isMobile = window.innerWidth <= 768;
        const page = link.dataset.page;
        const hasSubmenu = link.classList.contains('nav-item-with-submenu');

        if (isMobile && hasSubmenu) {
            // Toggle submenu on mobile
            const submenu = link.parentElement.querySelector('.mobile-submenu');
            const isCurrentlyOpen = submenu.classList.contains('open');

            // Close all other submenus
            document.querySelectorAll('.mobile-submenu').forEach(s => s.classList.remove('open'));
            document.querySelectorAll('.nav-item-with-submenu').forEach(l => l.classList.remove('open'));

            // Toggle current submenu
            if (!isCurrentlyOpen) {
                submenu.classList.add('open');
                link.classList.add('open');
            }
        } else {
            // Desktop behavior or mobile without submenu
            document.querySelectorAll('.main-nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            // Close mobile menu if no submenu
            if (!hasSubmenu) {
                mainNav.classList.remove('open');
            }

            // Update secondary nav (desktop only)
            if (!isMobile) {
                updateSecondaryNav(page);
            }
        }
    });
});

// Mobile submenu link clicks
document.querySelectorAll('.mobile-submenu a').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();

        // Update active state within submenu
        link.closest('.mobile-submenu').querySelectorAll('a').forEach(l => l.classList.remove('active'));
        link.classList.add('active');

        // Set parent as active
        document.querySelectorAll('.main-nav-link').forEach(l => l.classList.remove('active'));
        link.closest('.main-nav-item').querySelector('.main-nav-link').classList.add('active');

        // Close mobile menu
        mainNav.classList.remove('open');
    });
});

// Secondary navigation click handling
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('secondary-nav-link')) {
        e.preventDefault();
        document.querySelectorAll('.secondary-nav-link').forEach(l => l.classList.remove('active'));
        e.target.classList.add('active');
    }
});

function updateSecondaryNav(page) {
    const items = navData[page];

    if (items) {
        const html = items.map((item, i) =>
            `<li><a href="javascript:void(0)" class="secondary-nav-link ${i === 0 ? 'active' : ''}">${item}</a></li>`
        ).join('');

        secondaryNav.querySelector('.secondary-nav-list').innerHTML = html;
        secondaryNav.style.display = 'block';
    } else {
        secondaryNav.style.display = 'none';
    }
}