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

function getActiveSubpages(pageData) {
    return pageData.subPages.filter(subPage => !docMode || !subPage.skipInDocMode);
}

function generateNavigation(rootPage) {
    const mainNav = document.getElementById('mainNav');
    let html = '';

    // Loop through each page in navData
    for (const [slug, pageData] of Object.entries(navData)) {
        if (docMode && pageData.skipInDocMode) {
            continue;
        }

        const hasSubmenu = pageData.subPages && pageData.subPages.length > 0;
        const active = rootPage === slug;
        const open = isMobile() && active

        let submenuClass = hasSubmenu ? ' nav-item-with-submenu' : '';
        if (active) {
            submenuClass += ' active';
        }
        if (open) {
            submenuClass += ' open';
        }
        html += '<li class="main-nav-item">';
        html += `<a href="${pageData.link}" class="main-nav-link${submenuClass}" data-page="${slug}">${pageData.title}</a>`;

        // Add mobile submenu if subpages exist
        if (hasSubmenu) {
            html += `<div class="mobile-submenu ${open ? ' open' : ''}" id="submenu-${slug}">`;

            getActiveSubpages(pageData).forEach(function(subPage, i) {
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

    let secondaryList = secondaryNav.querySelector('.secondary-nav-list');
    let secondaryFromChild = secondaryNav.querySelector('.secondary-from-child');

    if (pageData && pageData.subPages) {
        const html = getActiveSubpages(pageData).map(function (subPage, i) {
            const activeClass = getActiveClass(i, subPage);
            return `<li class="secondary-nav-li"><a href="${subPage.link}" class="secondary-nav-link${activeClass}">${subPage.title}</a></li>`;
        }).join('');

        secondaryList.innerHTML = html;
        secondaryFromChild.style.display = 'none';
    } else {
        secondaryList.style.display = 'none';
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
const docMode = document.getElementById('header').dataset.docmode;

// generate nav
generateNavigation(rootPage);
generateSecondaryNavigation(rootPage);
addMenuEventHandlers();
