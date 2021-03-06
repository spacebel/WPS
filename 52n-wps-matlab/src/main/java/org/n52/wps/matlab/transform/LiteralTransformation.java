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
package org.n52.wps.matlab.transform;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import org.n52.matlab.connector.value.MatlabArray;
import org.n52.matlab.connector.value.MatlabBoolean;
import org.n52.matlab.connector.value.MatlabCell;
import org.n52.matlab.connector.value.MatlabDateTime;
import org.n52.matlab.connector.value.MatlabFile;
import org.n52.matlab.connector.value.MatlabMatrix;
import org.n52.matlab.connector.value.MatlabScalar;
import org.n52.matlab.connector.value.MatlabString;
import org.n52.matlab.connector.value.MatlabStruct;
import org.n52.matlab.connector.value.MatlabValue;
import org.n52.matlab.connector.value.ReturningMatlabValueVisitor;
import org.n52.wps.io.data.IData;


/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class LiteralTransformation {

    public abstract MatlabValue transformInput(IData data);

    public final IData transformOutput(MatlabValue value) {
        return value.accept(new ReturningMatlabValueVisitor<IData>() {
            @Override
            public IData visit(MatlabArray array) {
                return fromArray(array.value());
            }

            @Override
            public IData visit(MatlabBoolean bool) {
                return fromBoolean(bool.value());
            }

            @Override
            public IData visit(MatlabCell cell) {
                return fromCell(cell.value());
            }

            @Override
            public IData visit(MatlabMatrix matrix) {
                return fromMatrix(matrix.value());
            }

            @Override
            public IData visit(MatlabScalar scalar) {
                return fromScalar(scalar.value());
            }

            @Override
            public IData visit(MatlabString string) {
                return fromString(string.value());
            }

            @Override
            public IData visit(MatlabStruct struct) {
                return fromStruct(struct.value());
            }

            @Override
            public IData visit(MatlabFile file) {
                throw new IllegalArgumentException();
            }

            @Override
            public IData visit(MatlabDateTime time) {
                return fromDateTime(time.value());
            }
        });
    }

    protected IData fromScalar(double value) {
        throw new IllegalArgumentException();
    }

    protected IData fromArray(double[] value) {
        throw new IllegalArgumentException();
    }

    protected IData fromBoolean(boolean value) {
        throw new IllegalArgumentException();
    }

    protected IData fromCell(List<MatlabValue> value) {
        throw new IllegalArgumentException();
    }

    protected IData fromMatrix(double[][] value) {
        throw new IllegalArgumentException();
    }

    protected IData fromString(String value) {
        throw new IllegalArgumentException();
    }

    protected IData fromStruct(Map<MatlabString, MatlabValue> value) {
        throw new IllegalArgumentException();
    }

    protected IData fromDateTime(DateTime value) {
        throw new IllegalArgumentException();
    }

}
