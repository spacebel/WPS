/**
 * Copyright (C) 2013-2015 52°North Initiative for Geospatial Open Source
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
package org.n52.wps.matlab.description;

import org.n52.matlab.connector.value.MatlabType;
import org.n52.wps.io.data.IData;

import com.github.autermann.wps.commons.description.impl.LiteralInputDescriptionImpl;

import org.n52.wps.matlab.transform.LiteralType;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class MatlabLiteralInputDescription extends LiteralInputDescriptionImpl
        implements MatlabProcessInputDescription, MatlabLiteralTyped {
    private final LiteralType type;

    public MatlabLiteralInputDescription(AbstractMatlabLiteralInputDescriptionBuilder<?, ?> builder) {
        super(builder);
        this.type = builder.getType();
    }

    @Override
    public LiteralType getLiteralType() {
        return type;
    }

    @Override
    public MatlabType getMatlabType() {
        return this.type.getMatlabType();
    }

    @Override
    public Class<? extends IData> getBindingClass() {
        return this.type.getBindingClass();
    }

    public static AbstractMatlabLiteralInputDescriptionBuilder<?, ?> builder() {
        return new BuilderImpl();
    }

    private static class BuilderImpl extends AbstractMatlabLiteralInputDescriptionBuilder<MatlabLiteralInputDescription, BuilderImpl> {
        @Override
        public MatlabLiteralInputDescription build() {
            return new MatlabLiteralInputDescription(this);
        }
    }

}
