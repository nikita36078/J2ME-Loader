/*
 * Copyright 2023 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.microedition.location;

public class Landmark {
	private String name;
	private QualifiedCoordinates coordinates;
	private AddressInfo address;
	private String description;
	private byte dataValidity;
	LandmarkStore parentStore;

	public Landmark(String aName, String aDescription, QualifiedCoordinates someCoordinates,
			AddressInfo anAddressInfo) {
		if (aName == null) {
			throw new NullPointerException("The name is null");
		}
		this.name = aName;
		this.address = (anAddressInfo == null ? null : anAddressInfo.clone());
		this.coordinates = (someCoordinates == null ? null : someCoordinates.clone());
		this.description = aDescription;
		this.dataValidity = ((byte) (this.dataValidity | 0x2));
		this.dataValidity = ((byte) (this.dataValidity | 0x1));
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public QualifiedCoordinates getQualifiedCoordinates() {
		return this.coordinates;
	}

	public AddressInfo getAddressInfo() {
		return this.address;
	}

	public void setName(String name) {
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
		this.dataValidity = ((byte) (this.dataValidity | 0x1));
	}

	public void setQualifiedCoordinates(QualifiedCoordinates coordinates) {
		this.coordinates = coordinates;
	}

	public void setAddressInfo(AddressInfo addressInfo) {
		this.address = addressInfo;
		this.dataValidity = ((byte) (this.dataValidity | 0x2));
	}
}
