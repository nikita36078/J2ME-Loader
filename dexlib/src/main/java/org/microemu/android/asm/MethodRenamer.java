/*
 * Copyright 2026 Nikita Shakarun
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

package org.microemu.android.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renames methods that would otherwise override final methods of {@link Thread}.
 * <p>
 * CLDC has no {@code Thread.resume()}, {@code Thread.suspend()} and
 * {@code Thread.stop()}, so MIDlets may legally declare methods with these names
 * in a Thread subclass. On Android these methods are final and ART rejects such
 * a class while defining it:
 * <pre>
 * LinkageError: Method void g$a.resume() overrides final method in class java.lang.Thread
 * </pre>
 * Renaming the methods (and all references to them) keeps the MIDlet's own logic
 * without changing its behavior.
 */
public final class MethodRenamer {
	private static final String THREAD = "java/lang/Thread";
	private static final String RENAME_SUFFIX = "$j2me";
	private static final Set<String> FINAL_THREAD_METHODS = initFinalThreadMethods();

	private final Map<String, Map<String, String>> renames;

	private MethodRenamer(Map<String, Map<String, String>> renames) {
		this.renames = renames;
	}

	/**
	 * @return the new method name or {@code null} if the method should not be renamed
	 */
	public String getRenamedMethod(String owner, String name, String desc) {
		Map<String, String> methods = renames.get(owner);
		if (methods == null) {
			return null;
		}
		return methods.get(name + desc);
	}

	public static final class Builder {
		private final Map<String, String> superNames = new HashMap<>();
		private final Map<String, Set<String>> declaredMethods = new HashMap<>();

		public void addClass(byte[] classData) {
			ClassReader cr = new ClassReader(classData);
			String name = cr.getClassName();
			String superName = cr.getSuperName();
			Set<String> methods = new HashSet<>();
			cr.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String methodName, String descriptor,
												 String signature, String[] exceptions) {
					methods.add(methodName + descriptor);
					return null;
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			superNames.put(name, superName);
			declaredMethods.put(name, methods);
		}

		public MethodRenamer build() {
			Set<String> threadClasses = new HashSet<>();
			boolean changed = true;
			while (changed) {
				changed = false;
				for (Map.Entry<String, String> entry : superNames.entrySet()) {
					String superName = entry.getValue();
					if (THREAD.equals(superName) || threadClasses.contains(superName)) {
						if (threadClasses.add(entry.getKey())) {
							changed = true;
						}
					}
				}
			}

			Map<String, Map<String, String>> ownRenames = new HashMap<>();
			for (String className : threadClasses) {
				Set<String> methods = declaredMethods.get(className);
				if (methods == null) {
					continue;
				}
				for (String signature : methods) {
					if (!FINAL_THREAD_METHODS.contains(signature)) {
						continue;
					}
					int paren = signature.indexOf('(');
					String name = signature.substring(0, paren);
					String desc = signature.substring(paren);
					String newName = name + RENAME_SUFFIX;
					while (methods.contains(newName + desc)) {
						newName += '$';
					}
					Map<String, String> map = ownRenames.get(className);
					if (map == null) {
						map = new HashMap<>();
						ownRenames.put(className, map);
					}
					map.put(signature, newName);
				}
			}

			Map<String, Map<String, String>> renames = new HashMap<>();
			for (String className : threadClasses) {
				Map<String, String> map = new HashMap<>();
				String current = className;
				while (current != null) {
					Map<String, String> own = ownRenames.get(current);
					if (own != null) {
						for (Map.Entry<String, String> entry : own.entrySet()) {
							if (!map.containsKey(entry.getKey())) {
								map.put(entry.getKey(), entry.getValue());
							}
						}
					}
					current = superNames.get(current);
				}
				if (!map.isEmpty()) {
					renames.put(className, map);
				}
			}
			return new MethodRenamer(renames);
		}
	}

	private static Set<String> initFinalThreadMethods() {
		Set<String> methods = new HashSet<>();
		// Methods that exist in Java SE but not in CLDC.
		methods.add("resume()V");
		methods.add("suspend()V");
		methods.add("stop()V");
		methods.add("stop(Ljava/lang/Throwable;)V");
		methods.add("destroy()V");
		return methods;
	}
}
