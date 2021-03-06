<%--

    Copyright (C) 2012-2015 52�North Initiative for Geospatial Open Source
    Software GmbH

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License version 2 as published
    by the Free Software Foundation.

    If the program is linked with libraries which are licensed under one of
    the following licenses, the combination of the program with the linked
    library is not considered a "derivative work" of the program:

        - Apache License, version 2.0
        - Apache Software License, version 1.0
        - GNU Lesser General Public License, version 3
        - Mozilla Public License, versions 1.0, 1.1 and 2.0
        - Common Development and Distribution License (CDDL), version 1.0

    Therefore the distribution of the program linked with libraries licensed
    under the aforementioned licenses, is permitted by the copyright holders
    if the distribution is compliant with both the GNU General Public
    License version 2 and the aforementioned licenses.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
    Public License for more details.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="input" tagdir="/WEB-INF/tags"%>

<form:form id="customForm" modelAttribute="serviceProvider" method="POST" action="service_provider" class="form-horizontal">
	<legend>Info</legend>
	<input:customInput label="Provider Name" field="providerName" desc="Your or your company's name" required="false" />
	<input:customInput label="Provider Site" field="providerSite" desc="Your website" required="false" />
	<legend>Contact</legend>
	<input:customInput label="Responsible Person" field="individualName"
		desc="The name of the responsible person of this service" required="false" />
	<input:customInput label="Position" field="position" desc="The position of the responsible person" required="false" />
	<input:customInput label="Phone" field="phone" desc="The phone number of the responsible person" required="false" />
	<input:customInput label="Fax" field="facsimile" desc="The fax number of the responsible person" required="false" />
	<input:customInput label="Email" field="email" desc="The e-mail address of the responsible person" required="false" />
	<legend>Address</legend>
	<input:customInput label="Delivery Point" field="deliveryPoint" desc="The street address of the responsible person" required="false" />
	<input:customInput label="City" field="city" desc="The city the responsible person" required="false" />
	<input:customInput label="Administrative Area" field="administrativeArea"
		desc="The administrative area of the responsible person" required="false" />
	<input:customInput label="Postal Code" field="postalCode" desc="The postal code of the responsible person" required="false" />
	<input:customInput label="Country" field="country" desc="The country of the responsible person" required="false" />
	<input:customInput label="Save" type="submit" />
</form:form>
<script src="<c:url value="/static/js/custom.module.js" />"></script>