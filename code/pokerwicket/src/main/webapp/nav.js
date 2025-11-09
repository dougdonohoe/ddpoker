// Navigation data structure with proper links
const navData = {
    home: {
        title: 'Home',
        link: '/home',
        subPages: null
    },
    download: {
        title: 'Download',
        link: '/download',
        subPages: null
    },
    about: {
        title: 'About',
        link: '/about',
        subPages: [
            {title: 'Overview', link: '/about'},
            {title: 'Practice', link: '/about/practice'},
            {title: 'Online', link: '/about/online'},
            {title: 'Analysis', link: '/about/analysis'},
            {title: 'Poker Clock', link: '/about/pokerclock'},
            {title: 'Competition', link: '/about/competition'},
            {title: 'Screenshots', link: '/about/screenshots'},
            {title: 'FAQs', link: '/about/faq'}
        ]
    },
    store: {
        title: 'Store',
        link: '/store',
        subPages: [
            {title: 'Store', link: '/store'},
            {title: 'Donate', link: '/donate'},
        ]
    },
    forums: {
        title: 'Forums',
        link: '/forums',
        subPages: null
    },
    support: {
        title: 'Support',
        link: '/support',
        subPages: [
            {title: 'Overview', link: '/support'},
            {title: 'Self Help', link: '/support/selfhelp'},
            {title: 'Password Help', link: '/support/passwords'},
            {title: 'Online Supplement', link: '/support/onlinesupplement'},
        ]
    },
    online: {
        title: 'Online',
        link: '/online',
        subPages: [
            {title: 'Portal', link: '/online'},
            {title: 'Leaderboard', link: '/leaderboard'},
            {title: 'Current', link: '/current'},
            {title: 'Completed', link: '/completed'},
            {title: 'History', link: '/history'},
            {title: 'Search', link: '/search'},
            {title: 'Hosts', link: '/hosts'},
            {title: 'My Profile', link: '/myprofile'},
        ]
    },
    admin: {
        title: 'Admin',
        link: '/admin',
        subPages: [
            {title: 'Admin', link: '/admin'},
            {title: 'Profile Search', link: '/admin/online-profile-search'},
            {title: 'Reg Search', link: '/admin/reg-search'},
            {title: 'Ban List', link: '/admin/ban-list'},
        ]
    }
};

function isMobile() {
    return document.documentElement.clientWidth <= 768;
}

function generateNavigation(rootPage) {
    const mainNav = document.getElementById('mainNav');
    let html = '';

    // Loop through each page in navData
    for (const [slug, pageData] of Object.entries(navData)) {
        const hasSubmenu = pageData.subPages && pageData.subPages.length > 0;
        const submenuClass = hasSubmenu ? ' nav-item-with-submenu' : '';

        const active = rootPage === slug;
        const open = isMobile() && active

        let clazz = active ? ' active' : ''; // JDD: combine with submenuClass
        if (open) {
            clazz += ' open';
        }
        html += '<li class="main-nav-item">';
        html += `<a href="${pageData.link}" class="main-nav-link${submenuClass}${clazz}" data-page="${slug}">${pageData.title}</a>`;

        // Add mobile submenu if subpages exist
        if (hasSubmenu) {
            html += `<div class="mobile-submenu ${open ? ' open' : ''}" id="submenu-${slug}">`;
            pageData.subPages.forEach(function(subPage, i) {
                const fullMountPath = '/' + mountPath
                const active = i === 0 ? fullMountPath === subPage.link : fullMountPath.startsWith(subPage.link)
                const activeClass = active ? ' active' : '';
                html += `<a href="${subPage.link}" class="${activeClass}">${subPage.title}</a>`;
            });
            html += '</div>';
        }
        html += '</li>';
    }

    mainNav.innerHTML = html;
}

// JDD: duplicative of above - consolidate (one is full page secondary nav;other is mobile)
function generateSecondaryNavigation(root) {
    const secondaryNav = document.getElementById('secondaryNav');

    const pageData = navData[root];

    if (pageData && pageData.subPages) {
        const html = pageData.subPages.map(function (item, i) {
            // First item in list must match exactly, otherwise look at starts with
            // this prevents paths like 'about/online' from matching root 'about'
            const fullMountPath = '/' + mountPath
            const active = i === 0 ? fullMountPath === item.link : fullMountPath.startsWith(item.link)
            return '<li><a href="' + item.link + '" class="secondary-nav-link' + (active ? ' active' : '') + '">' + item.title + '</a></li>';
        }).join('');

        secondaryNav.querySelector('.secondary-nav-list').innerHTML = html;
        secondaryNav.style.display = 'block';
    } else {
        secondaryNav.style.display = 'none';
    }
}

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function () {
    // Set by TopNavigation
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const mainNav = document.getElementById('mainNav');

    // Mobile menu toggle
    mobileMenuToggle.addEventListener('click', function () {
        mainNav.classList.toggle('open');
    });

    // JDD: some click handling may be overkill since page is reloaded

    // Main navigation click handling
    document.querySelectorAll('.main-nav-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            const newRoot = link.dataset.root;
            const hasSubmenu = link.classList.contains('nav-item-with-submenu');

            if (isMobile() && hasSubmenu) {
                // don't follow link
                e.preventDefault();

                // Toggle submenu on mobile
                const submenu = link.parentElement.querySelector('.mobile-submenu');
                const isCurrentlyOpen = submenu.classList.contains('open');

                // Close all other submenus
                document.querySelectorAll('.mobile-submenu').forEach(function (s) {
                    s.classList.remove('open');
                });
                document.querySelectorAll('.nav-item-with-submenu').forEach(function (l) {
                    l.classList.remove('open');
                });

                // Toggle current submenu
                if (!isCurrentlyOpen) {
                    submenu.classList.add('open');
                    link.classList.add('open');
                }
            } else {
                // Desktop behavior or mobile without submenu
                document.querySelectorAll('.main-nav-link').forEach(function (l) {
                    l.classList.remove('active');
                });
                link.classList.add('active');

                // Close mobile menu if no submenu
                if (!hasSubmenu) {
                    mainNav.classList.remove('open');
                }

                // Update secondary nav (desktop only)
                if (!isMobile()) {
                    generateSecondaryNavigation(newRoot);
                }
            }
        });
    });

    // Secondary navigation click handling
    document.addEventListener('click', function (e) {
        if (e.target.classList.contains('secondary-nav-link')) {

            document.querySelectorAll('.secondary-nav-link').forEach(function (l) {
                l.classList.remove('active');
            });
            e.target.classList.add('active');
        }
    });
});

///
/// main script
///

// get wicket info
const mountPath = document.getElementById('header').dataset.mount;
const rootPage = document.getElementById('header').dataset.root;

// generate nav
generateNavigation(rootPage);
generateSecondaryNavigation(rootPage)
