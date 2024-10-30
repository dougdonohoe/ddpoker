/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2024 Doug Donohoe
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
package com.donohoedigital.wicket.annotations;

import com.donohoedigital.config.MatchingResources;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.component.IRequestablePage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This code was originally contributed (by me) to wicketstuff as the 'annotation'
 * library.  When 1.5 was released, its functionality was significantly
 * reduced, removing strategies that DD Poker relied on.  When I upgraded
 * to 1.5, I had to pull this in so we could restore the mount strategies
 * we previously used.  Wicket has moved on and there isn't much incentive
 * to re-introduce this functionality as a library, so keeping it internal.
 *
 * @author Doug Donohoe
 */
public class DDMountScanner {

    /**
     * Get the Spring search pattern given a package name or part of a package name.
     */
    public String getPatternForPackage(String packageName) {
        if (packageName == null) packageName = "";
        packageName = packageName.replace('.', '/');
        if (!packageName.endsWith("/")) {
            packageName += '/';
        }

        return "classpath*:" + packageName + "**/*.class";
    }

    /**
     * Scan given a package name or part of a package name and return list of classes with MountPath
     * annotation.
     */
    public List<Class<?>> getPackageMatches(String pattern) {
        return getPatternMatches(getPatternForPackage(pattern));
    }

    /**
     * Scan given a Spring search pattern and return list of classes with MountPath annotation.
     */
    public List<Class<?>> getPatternMatches(String pattern) {
        MatchingResources resources = new MatchingResources(pattern);
        Set<Class<?>> mounts = resources.getAnnotatedMatches(MountPath.class);
        for (Class<?> mount : mounts) {
            if (!(Page.class.isAssignableFrom(mount))) {
                throw new RuntimeException("@MountPath annotated class should subclass Page: " +
                        mount);
            }
        }
        return new ArrayList<>(mounts);
    }

    /**
     * Scan given package name or part of a package name.
     */
    public AnnotatedMountList scanPackage(String packageName) {
        return scanList(getPackageMatches(packageName));
    }

    /**
     * Scan a list of classes which are annotated with MountPath.
     */
    @SuppressWarnings({"unchecked"})
    protected AnnotatedMountList scanList(List<Class<?>> mounts) {
        AnnotatedMountList list = new AnnotatedMountList();
        for (Class<?> mount : mounts) {
            Class<? extends Page> page = (Class<? extends Page>) mount;
            scanClass(page, list);
        }
        return list;
    }

    /**
     * Magic of all this is done here.
     */
    private void scanClass(Class<? extends Page> pageClass, AnnotatedMountList list) {
        MountPath mountPath = pageClass.getAnnotation(MountPath.class);
        if (mountPath == null)
            return;

        // primary path, default if no explicit path is provided
        String path = mountPath.value();
        if (path == null || path.isEmpty()) {
            path = getDefaultMountPath(pageClass);
        }

        // See if we have mixed param info
        MountMixedParam mountMixedParam = pageClass.getAnnotation(MountMixedParam.class);
        String[] params = mountMixedParam == null ? null : mountMixedParam.parameterNames();

        // primary
        list.add(getRequestMapper(path, pageClass, params));

        // alternates
        for (String alt : mountPath.alt()) {
            list.add(getRequestMapper(alt, pageClass, params));
        }
    }

    /**
     * Returns the default mapper given a mount path and class.
     */
    public IRequestMapper getRequestMapper(String mountPath,
                                           Class<? extends IRequestablePage> pageClass,
                                           String[] parameterNames) {
        if (parameterNames == null || parameterNames.length == 0) {
            return new MountedMapper(mountPath, pageClass);
        } else {
            return new MountedMapper(mountPath, pageClass,
                    new MixedParamEncoder(parameterNames));
        }
    }

    /**
     * Returns the default mount path for a given class (used if the path has not been specified in
     * the <code>@MountPath</code> annotation). By default, this method returns the
     * pageClass.getSimpleName().
     */
    public String getDefaultMountPath(Class<? extends IRequestablePage> pageClass) {
        return pageClass.getSimpleName();
    }
}
