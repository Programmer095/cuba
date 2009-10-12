/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 27.08.2009 18:39:09
 *
 * $Id$
 */
package com.haulmont.cuba.web.sys;

import com.vaadin.terminal.gwt.server.ApplicationServlet;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.Browser;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;

public class CubaApplicationServlet extends ApplicationServlet {
    private static final long serialVersionUID = -8701539520754293569L;

    @Override
    protected boolean isTestingMode() {
        return ConfigProvider.getConfig(ServerConfig.class).getTestMode();
    }

    @Override
    protected WebApplicationContext getApplicationContext(HttpSession session) {
        return CubaApplicationContext.getApplicationContext(session);
    }

    @Override
    protected void writeAjaxPageHtmlHeader(HttpServletRequest request, BufferedWriter page, String title,
                                           String themeUri) throws IOException {
        page.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n");
        if (ConfigProvider.getConfig(WebConfig.class).getUseChromeFramePlugin()
                && Browser.getBrowserInfo(request).isChromeFrame()) {
            page.write("<meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\" />\n");
        }
        page.write("<style type=\"text/css\">"
                + "html, body {height:100%;}</style>");

        // Add favicon links
        page.write("<link rel=\"shortcut icon\" type=\"image/vnd.microsoft.icon\" href=\""
                        + themeUri + "/favicon.ico\" />");
        page.write("<link rel=\"icon\" type=\"image/vnd.microsoft.icon\" href=\""
                        + themeUri + "/favicon.ico\" />");

        page.write("<title>" + title + "</title>");
    }
}
