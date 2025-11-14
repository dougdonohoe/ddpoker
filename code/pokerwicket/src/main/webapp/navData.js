/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

// Menu navigation data
const navData = {
    home: {
        title: 'Home',
        link: '/home',
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
            {title: 'Competition', link: '/about/competition', skipInDocMode: true},
            {title: 'Screenshots', link: '/about/screenshots'},
            {title: 'FAQ', link: '/about/faq'}
        ]
    },
    download: {
        title: 'Download',
        link: '/download',
        subPages: null
    },
    store: {
        title: 'Donate',
        link: '/donate',
    },
    forums: {
        skipInDocMode: true,
        title: 'Forums',
        link: '/forums',
        subPages: null,
    },
    support: {
        skipInDocMode: true,
        title: 'Support',
        link: '/support',
        subPages: [
            {title: 'Overview', link: '/support'},
            {title: 'Self Help', link: '/support/selfhelp'},
            {title: 'Password Help', link: '/support/passwords', skipInDocMode: true},
            {title: 'Online Supplement', link: '/support/onlinesupplement'},
        ]
    },
    online: {
        skipInDocMode: true,
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
        skipInDocMode: true,
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

///
/// Node.js module export (only works in Node.js, ignored in browser)
///
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { navData };
}
