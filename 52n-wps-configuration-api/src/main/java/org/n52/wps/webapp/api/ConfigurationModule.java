/**
 * Copyright (C) 2007-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.webapp.api;

import java.util.List;

import org.n52.wps.webapp.api.types.BooleanConfigurationEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.DoubleConfigurationEntry;
import org.n52.wps.webapp.api.types.FileConfigurationEntry;
import org.n52.wps.webapp.api.types.IntegerConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;
import org.n52.wps.webapp.api.types.URIConfigurationEntry;

/**
 * Configuration modules in the application need to implement this interface to recognized and added to the application.
 * Spring will scan the context for any class implementing this interface and register it as a configuration module.
 */
public interface ConfigurationModule {
    /**
     * The name of the module which will appear on the user interface. (e.g. Grass Repository)
     *
     * @return the name of the configuration module
     */
    String getModuleName();

    /**
     * Get the active status of the module
     *
     * @return the status of the module
     */
    boolean isActive();

    /**
     * Set the module status to active/inactive
     *
     * @param active
     *            the new status
     */
    void setActive(boolean active);

    /**
     * Identify the category for the configuration module. See {@code ConfigurationCategory} for a list of avaliable
     * categories.
     *
     * @return the category for the configuration module
     */
    ConfigurationCategory getCategory();

    /**
     * List of all configurations entries for this configuration module. Configuration entries can be of type String,
     * Integer, Boolean, Double, File, and URI
     *
     * @return the list of configuration entries
     * @see StringConfigurationEntry
     * @see IntegerConfigurationEntry
     * @see BooleanConfigurationEntry
     * @see DoubleConfigurationEntry
     * @see FileConfigurationEntry
     * @see URIConfigurationEntry
     */
    List<? extends ConfigurationEntry<?>> getConfigurationEntries();

    /**
     * List of algorithms for this configuration module.
     *
     * @return the list of algorithms
     * @see AlgorithmEntry
     */
    List<AlgorithmEntry> getAlgorithmEntries();

    /**
     * List of formats for this configuration module.
     *
     * @return the list of formats
     * @see FormatEntry
     */
    List<FormatEntry> getFormatEntries();
}
