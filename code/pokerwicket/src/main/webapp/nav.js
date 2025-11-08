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

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function () {
    // Set by TopNavigation
    const mountPath = document.getElementById('header').dataset.mount;
    const rootPage = document.getElementById('header').dataset.root;
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const mainNav = document.getElementById('mainNav');
    const secondaryNav = document.getElementById('secondaryNav');

    // Mobile menu toggle
    mobileMenuToggle.addEventListener('click', function () {
        mainNav.classList.toggle('open');
    });

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
                    updateSecondaryNav(newRoot);
                }
            }
        });
    });

    // Mobile submenu link clicks
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

    // Secondary navigation click handling
    document.addEventListener('click', function (e) {
        if (e.target.classList.contains('secondary-nav-link')) {

            document.querySelectorAll('.secondary-nav-link').forEach(function (l) {
                l.classList.remove('active');
            });
            e.target.classList.add('active');
        }
    });

    function updateSecondaryNav(root) {
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

        // Find and activate mobile submenu link (if in mobile view)
        if (isMobile()) {
            document.querySelectorAll('.mobile-submenu a').forEach(function (link) {
                const linkPath = link.getAttribute('href');
                // JDD: this misses longer URLs with query params like /completed/-/-/...  (can't use i === 0 trick as above)
                const fullMountPath = '/' + mountPath
                if (fullMountPath === linkPath) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
        }
    }

    // Set root nav item active (called on page load)
    function updatePrimaryNav(root) {
        // Find and activate the main nav link
        document.querySelectorAll('.main-nav-link').forEach(function (link) {
            const submenu = link.parentElement.querySelector('.mobile-submenu');
            const hasSubmenu = link.classList.contains('nav-item-with-submenu');

            // If matching link, make active and open (mobile)
            if (root === link.dataset.root) {
                link.classList.add('active');

                // for mobile
                if (hasSubmenu && isMobile()) {
                    link.classList.add('open');
                    submenu.classList.add('open');
                }
            }
        });
    }

    // Set active links in root list and sub-pages
    updatePrimaryNav(rootPage);
    updateSecondaryNav(rootPage)

    // JDD: Needed? Optional: Also call on window resize in case user switches between mobile/desktop
    //window.addEventListener('resize', updatePrimaryNav);
});