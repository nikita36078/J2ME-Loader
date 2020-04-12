package com.mascotcapsule.micro3d.v3.util;

import java.util.HashSet;

public abstract class ObjectPool<T> {

	private HashSet<T> available = new HashSet<>();
	private HashSet<T> inUse = new HashSet<>();

	public ObjectPool(int minCapacity) {
		for (int i = 0; i < minCapacity; i++) {
			T t = create();
			available.add(t);
		}
	}

	public T get() {
		if (available.isEmpty()) {
			available.add(create());
		}
		T t = available.iterator().next();
		available.remove(t);
		inUse.add(t);
		return t;
	}

	public void release(T t) {
		inUse.remove(t);
		available.add(t);
	}

	public abstract T create();
}
