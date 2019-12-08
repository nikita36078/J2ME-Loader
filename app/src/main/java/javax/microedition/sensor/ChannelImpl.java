/*
 * Copyright 2019 Nikita Shakarun
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

package javax.microedition.sensor;

public class ChannelImpl implements Channel {
	private ChannelInfo info;

	public ChannelImpl(ChannelInfo info) {
		this.info = info;
	}

	@Override
	public void addCondition(ConditionListener listener, Condition condition) {

	}

	@Override
	public ChannelInfo getChannelInfo() {
		return info;
	}

	@Override
	public String getChannelUrl() {
		return null;
	}

	@Override
	public Condition[] getConditions(ConditionListener listener) {
		return new Condition[0];
	}

	@Override
	public void removeAllConditions() {

	}

	@Override
	public void removeCondition(ConditionListener listener, Condition condition) {

	}

	@Override
	public void removeConditionListener(ConditionListener listener) {

	}
}
