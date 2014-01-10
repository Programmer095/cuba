/*
 * Copyright 2010 IT Mill Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.terminal.gwt.client.ui;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.*;
import com.vaadin.terminal.gwt.client.ui.layout.ChildComponentContainer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VEmbedded extends HTML implements Paintable {
    public static final String CLICK_EVENT_IDENTIFIER = "click";

    private static String CLASSNAME = "v-embedded";

    private String height;
    private String width;
    private Element browserElement;

    private String type;

    private ApplicationConnection client;

    private final ClickEventHandler clickEventHandler = new ClickEventHandler(
            this, CLICK_EVENT_IDENTIFIER) {

        @Override
        protected <H extends EventHandler> HandlerRegistration registerHandler(
                H handler, Type<H> type) {
            return addDomHandler(handler, type);
        }

    };

    public VEmbedded() {
        setStyleName(CLASSNAME);
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        if (client.updateComponent(this, uidl, true)) {
            return;
        }
        this.client = client;

        boolean clearBrowserElement = true;

        clickEventHandler.handleEventHandlerRegistration(client);

        if (uidl.hasAttribute("type")) {
            type = uidl.getStringAttribute("type");
            if (type.equals("image")) {
                addStyleName(CLASSNAME + "-image");
                Element el = null;
                boolean created = false;
                NodeList<Node> nodes = getElement().getChildNodes();
                if (nodes != null && nodes.getLength() == 1) {
                    Node n = nodes.getItem(0);
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) n;
                        if (e.getTagName().equals("IMG")) {
                            el = e;
                        }
                    }
                }
                if (el == null) {
                    setHTML("");
                    el = DOM.createImg();
                    created = true;
                    client.addPngFix(el);
                    DOM.sinkEvents(el, Event.ONLOAD);
                }

                // Set attributes
                Style style = el.getStyle();
                String w = uidl.getStringAttribute("width");
                if (w != null) {
                    style.setProperty("width", w);
                } else {
                    style.setProperty("width", "");
                }
                String h = uidl.getStringAttribute("height");
                if (h != null) {
                    style.setProperty("height", h);
                } else {
                    style.setProperty("height", "");
                }
                DOM.setElementProperty(el, "src", getSrc(uidl, client));

                if (created) {
                    // insert in dom late
                    getElement().appendChild(el);
                }

                /*
                 * Sink tooltip events so tooltip is displayed when hovering the
                 * image.
                 */
                sinkEvents(VTooltip.TOOLTIP_EVENTS);

            } else if (type.equals("browser")) {
                addStyleName(CLASSNAME + "-browser");
                if (browserElement == null) {
                    setHTML("<iframe width=\"100%\" height=\"100%\" frameborder=\"0\" allowTransparency=\"true\" src=\""
                            + getSrc(uidl, client)
                            + "\" name=\""
                            + uidl.getId() + "\"></iframe>");
                    browserElement = DOM.getFirstChild(getElement());
                } else {
                    DOM.setElementAttribute(browserElement, "src",
                            getSrc(uidl, client));
                }
                clearBrowserElement = false;
            } else {
                VConsole.log("Unknown Embedded type '" + type + "'");
            }
        } else if (uidl.hasAttribute("mimetype")) {
            final String mime = uidl.getStringAttribute("mimetype");
            if (mime.equals("application/x-shockwave-flash")) {
                // Handle embedding of Flash
                setHTML(createFlashEmbed(uidl));

            } else if (mime.equals("image/svg+xml")) {
                addStyleName(CLASSNAME + "-svg");
                String data;
                Map<String, String> parameters = getParameters(uidl);
                if (parameters.get("data") == null) {
                    data = getSrc(uidl, client);
                } else {
                    data = "data:image/svg+xml," + parameters.get("data");
                }
                setHTML("");
                ObjectElement obj = Document.get().createObjectElement();
                obj.setType(mime);
                obj.setData(data);
                if (width != null) {
                    obj.getStyle().setProperty("width", "100%");
                }
                if (height != null) {
                    obj.getStyle().setProperty("height", "100%");
                }
                if (uidl.hasAttribute("classid")) {
                    obj.setAttribute("classid",
                            uidl.getStringAttribute(escapeAttribute("classid")));
                }
                if (uidl.hasAttribute("codebase")) {
                    obj.setAttribute("codebase", uidl
                            .getStringAttribute(escapeAttribute("codebase")));
                }
                if (uidl.hasAttribute("codetype")) {
                    obj.setAttribute("codetype", uidl
                            .getStringAttribute(escapeAttribute("codetype")));
                }
                if (uidl.hasAttribute("archive")) {
                    obj.setAttribute("archive",
                            uidl.getStringAttribute(escapeAttribute("archive")));
                }
                if (uidl.hasAttribute("standby")) {
                    obj.setAttribute("standby",
                            uidl.getStringAttribute(escapeAttribute("standby")));
                }
                getElement().appendChild(obj);

            } else {
                VConsole.log("Unknown Embedded mimetype '" + mime + "'");
            }
        } else {
            VConsole.log("Unknown Embedded; no type or mimetype attribute");
        }

        if (clearBrowserElement) {
            browserElement = null;
        }
    }

    /**
     * Creates the Object and Embed tags for the Flash plugin so it works
     * cross-browser
     *
     * @param uidl
     *            The UIDL
     * @return Tags concatenated into a string
     */
    private String createFlashEmbed(UIDL uidl) {
        addStyleName(CLASSNAME + "-flash");

        /*
         * To ensure cross-browser compatibility we are using the twice-cooked
         * method to embed flash i.e. we add a OBJECT tag for IE ActiveX and
         * inside it a EMBED for all other browsers.
         */

        StringBuilder html = new StringBuilder();

        // Start the object tag
        html.append("<object ");

        /*
         * Add classid required for ActiveX to recognize the flash. This is a
         * predefined value which ActiveX recognizes and must be the given
         * value. More info can be found on
         * http://kb2.adobe.com/cps/415/tn_4150.html. Allow user to override
         * this by setting his own classid.
         */
        if (uidl.hasAttribute("classid")) {
            html.append("classid=\""
                    + escapeAttribute(uidl.getStringAttribute("classid"))
                    + "\" ");
        } else {
            html.append("classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" ");
        }

        /*
         * Add codebase required for ActiveX and must be exactly this according
         * to http://kb2.adobe.com/cps/415/tn_4150.html to work with the above
         * given classid. Again, see more info on
         * http://kb2.adobe.com/cps/415/tn_4150.html. Limiting Flash version to
         * 6.0.0.0 and above. Allow user to override this by setting his own
         * codebase
         */
        if (uidl.hasAttribute("codebase")) {
            html.append("codebase=\""
                    + escapeAttribute(uidl.getStringAttribute("codebase"))
                    + "\" ");
        } else {
            html.append("codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,0,0\" ");
        }

        // Add width and height
        html.append("width=\"" + width + "\" ");
        html.append("height=\"" + height + "\" ");
        html.append("type=\"application/x-shockwave-flash\" ");

        // Codetype
        if (uidl.hasAttribute("codetype")) {
            html.append("codetype=\"" + uidl.getStringAttribute("codetype")
                    + "\" ");
        }

        // Standby
        if (uidl.hasAttribute("standby")) {
            html.append("standby=\"" + uidl.getStringAttribute("standby")
                    + "\" ");
        }

        // Archive
        if (uidl.hasAttribute("archive")) {
            html.append("archive=\"" + uidl.getStringAttribute("archive")
                    + "\" ");
        }

        // End object tag
        html.append(">");

        // Ensure we have an movie parameter
        Map<String, String> parameters = getParameters(uidl);
        if (parameters.get("movie") == null) {
            parameters.put("movie", getSrc(uidl, client));
        }

        // Add parameters to OBJECT
        for (String name : parameters.keySet()) {
            html.append("<param ");
            html.append("name=\"" + escapeAttribute(name) + "\" ");
            html.append("value=\"" + escapeAttribute(parameters.get(name))
                    + "\" ");
            html.append("/>");
        }

        // Build inner EMBED tag
        html.append("<embed ");
        html.append("src=\"" + getSrc(uidl, client) + "\" ");
        html.append("width=\"" + width + "\" ");
        html.append("height=\"" + height + "\" ");
        html.append("type=\"application/x-shockwave-flash\" ");

        // Add the parameters to the Embed
        for (String name : parameters.keySet()) {
            html.append(escapeAttribute(name));
            html.append("=");
            html.append("\"" + escapeAttribute(parameters.get(name)) + "\"");
        }

        // End embed tag
        html.append("></embed>");

        // End object tag
        html.append("</object>");

        return html.toString();
    }

    /**
     * Escapes the string so it is safe to write inside an HTML attribute.
     *
     * @param attribute
     *            The string to escape
     * @return An escaped version of <literal>attribute</literal>.
     */
    private String escapeAttribute(String attribute) {
        attribute = attribute.replace("\"", "&quot;");
        attribute = attribute.replace("'", "&#39;");
        attribute = attribute.replace(">", "&gt;");
        attribute = attribute.replace("<", "&lt;");
        attribute = attribute.replace("&", "&amp;");
        return attribute;
    }

    /**
     * Returns a map (name -> value) of all parameters in the UIDL.
     *
     * @param uidl
     * @return
     */
    private static Map<String, String> getParameters(UIDL uidl) {
        Map<String, String> parameters = new HashMap<String, String>();

        Iterator<Object> childIterator = uidl.getChildIterator();
        while (childIterator.hasNext()) {

            Object child = childIterator.next();
            if (child instanceof UIDL) {

                UIDL childUIDL = (UIDL) child;
                if (childUIDL.getTag().equals("embeddedparam")) {
                    String name = childUIDL.getStringAttribute("name");
                    String value = childUIDL.getStringAttribute("value");
                    parameters.put(name, value);
                }
            }

        }

        return parameters;
    }

    /**
     * Helper to return translated src-attribute from embedded's UIDL
     *
     * @param uidl
     * @param client
     * @return
     */
    private String getSrc(UIDL uidl, ApplicationConnection client) {
        String url = client.translateVaadinUri(uidl.getStringAttribute("src"));
        if (url == null) {
            return "";
        }
        return url;
    }

    @Override
    public void setWidth(String width) {
        this.width = width;
        if (isDynamicHeight()) {
            int oldHeight = getOffsetHeight();
            super.setWidth(width);
            int newHeight = getOffsetHeight();
            /*
             * Must notify parent if the height changes as a result of a width
             * change
             */
            if (oldHeight != newHeight) {
                Util.notifyParentOfSizeChange(this, false);
            }
        } else {
            super.setWidth(width);
        }

    }

    private boolean isDynamicWidth() {
        return width == null || width.equals("");
    }

    private boolean isDynamicHeight() {
        return height == null || height.equals("");
    }

    @Override
    public void setHeight(String height) {
        this.height = height;
        super.setHeight(height);
    }

    @Override
    protected void onDetach() {
        if (BrowserInfo.get().isIE()) {
            // Force browser to fire unload event when component is detached
            // from the view (IE doesn't do this automatically)
            if (browserElement != null) {
                DOM.setElementAttribute(browserElement, "src",
                        "javascript:false");
            }
        }
        super.onDetach();
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (DOM.eventGetType(event) == Event.ONLOAD) {
            if ("image".equals(type)) {
                updateElementDynamicSizeFromImage();

                if (isDynamicHeight() || isDynamicWidth()) {
                    // CAUTION! trigger updating parent height manually
                    final Widget self = this;

                    Widget parent = self.getParent();
                    while (parent != null && !(parent instanceof Container)) {
                        parent = parent.getParent();
                    }

                    if (self.getParent() instanceof ChildComponentContainer) {
                        ChildComponentContainer childComponentContainer = (ChildComponentContainer) self.getParent();

                        int w = Util.getRequiredWidth(getElement());
                        int h = Util.getRequiredHeight(getElement());

                        childComponentContainer.updateWidgetSize(w, h);
                        childComponentContainer.setContainerSize(w, h);
                    }

                    if (parent instanceof VOrderedLayout) {
                        VOrderedLayout orderedLayout = (VOrderedLayout) parent;

                        orderedLayout.layoutSizeMightHaveChanged();
                        orderedLayout.calculateAlignments();
                        orderedLayout.setRootSize();
                    }
                } else {
                    Util.notifyParentOfSizeChange(this, true);
                }
            }
        }

        client.handleTooltipEvent(event, this);
    }

    /**
     * Updates the size of the embedded component's element if size is
     * undefined. Without this embeddeds containing images will remain the wrong
     * size in certain cases (e.g. #6304).
     */
    private void updateElementDynamicSizeFromImage() {
        if (isDynamicWidth()) {
            getElement().getStyle().setWidth(
                    getElement().getFirstChildElement().getOffsetWidth(),
                    Unit.PX);
        }
        if (isDynamicHeight()) {
            getElement().getStyle().setHeight(
                    getElement().getFirstChildElement().getOffsetHeight(),
                    Unit.PX);
        }
    }
}