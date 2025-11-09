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

function getActiveClass(i, subPage) {
    // First item in list must match exactly, otherwise look at starts with
    // this prevents paths like 'about/online' from matching root 'about'
    const fullMountPath = '/' + mountPath
    const active = i === 0 ? fullMountPath === subPage.link : fullMountPath.startsWith(subPage.link)
    const activeClass = active ? ' active' : '';
    return activeClass;
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
                const activeClass = getActiveClass(i, subPage);
                html += `<a href="${subPage.link}" class="${activeClass}">${subPage.title}</a>`;
            });
            html += '</div>';
        }
        html += '</li>';
    }

    mainNav.innerHTML = html;
}

function generateSecondaryNavigation(root) {
    const secondaryNav = document.getElementById('secondaryNav');
    const pageData = navData[root];

    if (pageData && pageData.subPages) {
        const html = pageData.subPages.map(function (subPage, i) {
            const activeClass = getActiveClass(i, subPage);
            return `<li><a href="${subPage.link}" class="secondary-nav-link${activeClass}">${subPage.title}</a></li>`;
        }).join('');

        secondaryNav.querySelector('.secondary-nav-list').innerHTML = html;
        secondaryNav.style.display = 'block';
    } else {
        secondaryNav.style.display = 'none';
    }
}

// Menu event handlers
function addMenuEventHandlers() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const mainNav = document.getElementById('mainNav');

    // Mobile menu toggle
    mobileMenuToggle.addEventListener('click', function () {
        mainNav.classList.toggle('open');
    });

    // Mobile menu open/close handling
    document.querySelectorAll('.main-nav-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
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
            }
        });
    });
}

///
/// main script
///

// get wicket info
const mountPath = document.getElementById('header').dataset.mount;
const rootPage = document.getElementById('header').dataset.root;

// generate nav
generateNavigation(rootPage);
generateSecondaryNavigation(rootPage);
addMenuEventHandlers();
