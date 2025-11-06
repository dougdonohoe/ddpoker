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
            {title: 'My Profile', link: '/myprofile'},
        ]
    },
    admin: {
        title: 'Admin',
        link: '/admin',
        subPages: [
            {title: 'Admin', link: '/Admin'},
            {title: 'Profile Search', link: '/admin/online-profile-search'},
            {title: 'Reg Search', link: '/admin/reg-search'},
            {title: 'Ban List', link: '/admin/ban-list'},
        ]
    }
};

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function () {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const mainNav = document.getElementById('mainNav');
    const secondaryNav = document.getElementById('secondaryNav');

    // Mobile menu toggle
    mobileMenuToggle.addEventListener('click', function () {
        mainNav.classList.toggle('open');
    });

    // Main navigation click handling (for demo - prevent navigation)
    document.querySelectorAll('.main-nav-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            const isMobile = document.documentElement.clientWidth <= 768;
            const page = link.dataset.page;
            const hasSubmenu = link.classList.contains('nav-item-with-submenu');

            if (isMobile && hasSubmenu) {
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
                if (!isMobile) {
                    updateSecondaryNav(page);
                }
            }
        });
    });

    // Mobile submenu link clicks (for demo - prevent navigation)
    document.querySelectorAll('.mobile-submenu a').forEach(function (link) {
        link.addEventListener('click', function (e) {

            // Update active state within submenu
            link.closest('.mobile-submenu').querySelectorAll('a').forEach(function (l) {
                l.classList.remove('active');
            });
            link.classList.add('active');

            // Set parent as active
            document.querySelectorAll('.main-nav-link').forEach(function (l) {
                l.classList.remove('active');
            });
            link.closest('.main-nav-item').querySelector('.main-nav-link').classList.add('active');

            // Close mobile menu
            mainNav.classList.remove('open');
        });
    });

    // Secondary navigation click handling (for demo - prevent navigation)
    document.addEventListener('click', function (e) {
        if (e.target.classList.contains('secondary-nav-link')) {

            document.querySelectorAll('.secondary-nav-link').forEach(function (l) {
                l.classList.remove('active');
            });
            e.target.classList.add('active');
        }
    });

    function updateSecondaryNav(page) {
        const pageData = navData[page];

        if (pageData && pageData.subPages) {
            const html = pageData.subPages.map(function (item, i) {
                return '<li><a href="' + item.link + '" class="secondary-nav-link' + (i === 0 ? ' active' : '') + '">' + item.title + '</a></li>';
            }).join('');

            secondaryNav.querySelector('.secondary-nav-list').innerHTML = html;
            secondaryNav.style.display = 'block';
        } else {
            secondaryNav.style.display = 'none';
        }
    }

    // Set active states based on current URL path
    function setActiveNavigation() {
        const currentPath = window.location.pathname;

        // Find and activate the main nav link
        document.querySelectorAll('.main-nav-link').forEach(function (link) {
            const linkPath = link.getAttribute('href');
            const page = link.dataset.page;

            // Check if current path matches or starts with this section
            if (currentPath === linkPath || currentPath.startsWith(linkPath + '/')) {
                link.classList.add('active');

                // If on desktop and this section has subpages, update secondary nav
                if (document.documentElement.clientWidth > 768 && navData[page] && navData[page].subPages) {
                    updateSecondaryNav(page);
                }
            } else {
                link.classList.remove('active');
            }
        });

        // Find and activate the secondary nav link (if visible)
        if (document.documentElement.clientWidth > 768) {
            document.querySelectorAll('.secondary-nav-link').forEach(function (link) {
                const linkPath = link.getAttribute('href');
                if (currentPath === linkPath) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
        }

        // Find and activate mobile submenu link (if in mobile view)
        if (document.documentElement.clientWidth <= 768) {
            document.querySelectorAll('.mobile-submenu a').forEach(function (link) {
                const linkPath = link.getAttribute('href');
                if (currentPath === linkPath) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
        }
    }

    // set secondary nav on load
    updateSecondaryNav("about") // JDD: need to determine active page

    // Call it on page load
    setActiveNavigation();

    // Optional: Also call on window resize in case user switches between mobile/desktop
    //window.addEventListener('resize', setActiveNavigation);
});