/*
 * Copyright 2020 Nikita Shakarun
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

package javax.microedition.amms;

public interface MediaProcessorListener {
	public static final String PROCESSOR_REALIZED = "processRealized";
	public static final String PROCESSING_STARTED = "processingStarted";
	public static final String PROCESSING_STOPPED = "processingStopped";
	public static final String PROCESSING_ABORTED = "processingAborted";
	public static final String PROCESSING_COMPLETED = "processingCompleted";
	public static final String PROCESSING_ERROR = "processingError";

	public void mediaProcessorUpdate(MediaProcessor processor, String event, Object eventData);
}
