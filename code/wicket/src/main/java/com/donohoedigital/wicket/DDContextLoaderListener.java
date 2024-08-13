package com.donohoedigital.wicket;

import com.donohoedigital.config.SpringHack;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Need to subclass normal ContextLoaderListener so we can invoke our SpringHack
 * before anything gets loaded in webapps.
 */
public class DDContextLoaderListener extends ContextLoaderListener {
    static {
        SpringHack.initialize();
    }

    public DDContextLoaderListener() {
        super();
    }
}
