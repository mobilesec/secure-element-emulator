
package at.mroland.objectstaterecovery.helper;

/** An unordered map where identity comparison is used for keys and the values are ints. This implementation is a cuckoo hash map
 * using 3 hashes, random walking, and a small stash for problematic keys. Null keys are not allowed. No allocation is done except
 * when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * @author Nathan Sweet */
public class IdentityObjectIntMap<K> {
	private static final int PRIME1 = 0xbe1f14b1;
	private static final int PRIME2 = 0xb4b82e39;
	private static final int PRIME3 = 0xced1c241;

	public int size;

	private K[] mKeyTable;
	private int[] mValueTable;
	int capacity, stashSize;

	private float mLoadFactor;
	private int mHashShift, mMask, mThreshold;
	private int mStashCapacity;
	private int mPushIterations;

	/** Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
	 * backing table. */
	public IdentityObjectIntMap () {
		this(32, 0.8f);
	}

	/** Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
	 * table. */
	public IdentityObjectIntMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor items
	 * before growing the backing table. */
	public IdentityObjectIntMap (int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		if (capacity > 1 << 30) throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		capacity = ObjectMap.nextPowerOfTwo(initialCapacity);

		if (loadFactor <= 0) throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		this.mLoadFactor = loadFactor;

		mThreshold = (int)(capacity * loadFactor);
		mMask = capacity - 1;
		mHashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		mStashCapacity = Math.max(3, (int)Math.ceil(Math.log(capacity)) * 2);
		mPushIterations = Math.max(Math.min(capacity, 8), (int)Math.sqrt(capacity) / 8);

		mKeyTable = (K[])new Object[capacity + mStashCapacity];
		mValueTable = new int[mKeyTable.length];
	}

	public void put (K key, int value) {
		if (key == null) throw new IllegalArgumentException("key cannot be null.");
		K[] keyTable = this.mKeyTable;

		// Check for existing keys.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mMask;
		K key1 = keyTable[index1];
		if (key == key1) {
			mValueTable[index1] = value;
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key == key2) {
			mValueTable[index2] = value;
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key == key3) {
			mValueTable[index3] = value;
			return;
		}

		// Update key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (keyTable[i] == key) {
				mValueTable[i] = value;
				return;
			}
		}

		// Check for empty buckets.
		if (key1 == null) {
			keyTable[index1] = key;
			mValueTable[index1] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		if (key2 == null) {
			keyTable[index2] = key;
			mValueTable[index2] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		if (key3 == null) {
			keyTable[index3] = key;
			mValueTable[index3] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	/** Skips checks for existing keys. */
	private void putResize (K key, int value) {
		// Check for empty buckets.
		int hashCode = System.identityHashCode(key);
		int index1 = hashCode & mMask;
		K key1 = mKeyTable[index1];
		if (key1 == null) {
			mKeyTable[index1] = key;
			mValueTable[index1] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = mKeyTable[index2];
		if (key2 == null) {
			mKeyTable[index2] = key;
			mValueTable[index2] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = mKeyTable[index3];
		if (key3 == null) {
			mKeyTable[index3] = key;
			mValueTable[index3] = value;
			if (size++ >= mThreshold) resize(capacity << 1);
			return;
		}

		push(key, value, index1, key1, index2, key2, index3, key3);
	}

	private void push (K insertKey, int insertValue, int index1, K key1, int index2, K key2, int index3, K key3) {
		K[] keyTable = this.mKeyTable;
		int[] valueTable = this.mValueTable;
		int mask = this.mMask;

		// Push keys until an empty bucket is found.
		K evictedKey;
		int evictedValue;
		int i = 0, pushIterations = this.mPushIterations;
		do {
			// Replace the key and value for one of the hashes.
			switch (ObjectMap.random.nextInt(3)) {
			case 0:
				evictedKey = key1;
				evictedValue = valueTable[index1];
				keyTable[index1] = insertKey;
				valueTable[index1] = insertValue;
				break;
			case 1:
				evictedKey = key2;
				evictedValue = valueTable[index2];
				keyTable[index2] = insertKey;
				valueTable[index2] = insertValue;
				break;
			default:
				evictedKey = key3;
				evictedValue = valueTable[index3];
				keyTable[index3] = insertKey;
				valueTable[index3] = insertValue;
				break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			int hashCode = System.identityHashCode(evictedKey);
			index1 = hashCode & mask;
			key1 = keyTable[index1];
			if (key1 == null) {
				keyTable[index1] = evictedKey;
				valueTable[index1] = evictedValue;
				if (size++ >= mThreshold) resize(capacity << 1);
				return;
			}

			index2 = hash2(hashCode);
			key2 = keyTable[index2];
			if (key2 == null) {
				keyTable[index2] = evictedKey;
				valueTable[index2] = evictedValue;
				if (size++ >= mThreshold) resize(capacity << 1);
				return;
			}

			index3 = hash3(hashCode);
			key3 = keyTable[index3];
			if (key3 == null) {
				keyTable[index3] = evictedKey;
				valueTable[index3] = evictedValue;
				if (size++ >= mThreshold) resize(capacity << 1);
				return;
			}

			if (++i == pushIterations) break;

			insertKey = evictedKey;
			insertValue = evictedValue;
		} while (true);

		putStash(evictedKey, evictedValue);
	}

	private void putStash (K key, int value) {
		if (stashSize == mStashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			put(key, value);
			return;
		}
		// Store key in the stash.
		int index = capacity + stashSize;
		mKeyTable[index] = key;
		mValueTable[index] = value;
		stashSize++;
		size++;
	}

	/** @param defaultValue Returned if the key was not associated with a value. */
	public int get (K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mMask;
		if (key != mKeyTable[index]) {
			index = hash2(hashCode);
			if (key != mKeyTable[index]) {
				index = hash3(hashCode);
				if (key != mKeyTable[index]) return getStash(key, defaultValue);
			}
		}
		return mValueTable[index];
	}

	private int getStash (K key, int defaultValue) {
		K[] keyTable = this.mKeyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
            if (key == keyTable[i]) return mValueTable[i];
        }
		return defaultValue;
	}

	/** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map. */
	public int getAndIncrement (K key, int defaultValue, int increment) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mMask;
		if (key != mKeyTable[index]) {
			index = hash2(hashCode);
			if (key != mKeyTable[index]) {
				index = hash3(hashCode);
				if (key != mKeyTable[index]) return getAndIncrementStash(key, defaultValue, increment);
			}
		}
		int value = mValueTable[index];
		mValueTable[index] = value + increment;
		return value;
	}

	private int getAndIncrementStash (K key, int defaultValue, int increment) {
		K[] keyTable = this.mKeyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
            if (key == keyTable[i]) {
                int value = mValueTable[i];
                mValueTable[i] = value + increment;
                return value;
            }
        }
		put(key, defaultValue + increment);
		return defaultValue;
	}

	public int remove (K key, int defaultValue) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mMask;
		if (key == mKeyTable[index]) {
			mKeyTable[index] = null;
			int oldValue = mValueTable[index];
			size--;
			return oldValue;
		}

		index = hash2(hashCode);
		if (key == mKeyTable[index]) {
			mKeyTable[index] = null;
			int oldValue = mValueTable[index];
			size--;
			return oldValue;
		}

		index = hash3(hashCode);
		if (key == mKeyTable[index]) {
			mKeyTable[index] = null;
			int oldValue = mValueTable[index];
			size--;
			return oldValue;
		}

		return removeStash(key, defaultValue);
	}

	int removeStash (K key, int defaultValue) {
		K[] keyTable = this.mKeyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key == keyTable[i]) {
				int oldValue = mValueTable[i];
				removeStashIndex(i);
				size--;
				return oldValue;
			}
		}
		return defaultValue;
	}

	void removeStashIndex (int index) {
		// If the removed location was not last, move the last tuple to the removed location.
		stashSize--;
		int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			mKeyTable[index] = mKeyTable[lastIndex];
			mValueTable[index] = mValueTable[lastIndex];
		}
	}

	/** Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
	 * done. If the map contains more items than the specified capacity, nothing is done. */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		if (size > maximumCapacity) maximumCapacity = size;
		if (capacity <= maximumCapacity) return;
		maximumCapacity = ObjectMap.nextPowerOfTwo(maximumCapacity);
		resize(maximumCapacity);
	}

	/** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
	public void clear (int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity);
	}

	public void clear () {
		K[] keyTable = this.mKeyTable;
		for (int i = capacity + stashSize; i-- > 0;) {
            keyTable[i] = null;
        }
		size = 0;
		stashSize = 0;
	}

	/** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be
	 * an expensive operation. */
	public boolean containsValue (int value) {
		int[] valueTable = this.mValueTable;
		for (int i = capacity + stashSize; i-- > 0;) {
            if (valueTable[i] == value) return true;
        }
		return false;
	}

	public boolean containsKey (K key) {
		int hashCode = System.identityHashCode(key);
		int index = hashCode & mMask;
		if (key != mKeyTable[index]) {
			index = hash2(hashCode);
			if (key != mKeyTable[index]) {
				index = hash3(hashCode);
				if (key != mKeyTable[index]) return containsKeyStash(key);
			}
		}
		return true;
	}

	private boolean containsKeyStash (K key) {
		K[] keyTable = this.mKeyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
            if (key == keyTable[i]) return true;
        }
		return false;
	}

	/** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation. */
	public K findKey (int value) {
		int[] valueTable = this.mValueTable;
		for (int i = capacity + stashSize; i-- > 0;) {
            if (valueTable[i] == value) return mKeyTable[i];
        }
		return null;
	}

	/** Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes. */
	public void ensureCapacity (int additionalCapacity) {
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= mThreshold) resize(ObjectMap.nextPowerOfTwo((int)(sizeNeeded / mLoadFactor)));
	}

	private void resize (int newSize) {
		int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		mThreshold = (int)(newSize * mLoadFactor);
		mMask = newSize - 1;
		mHashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		mStashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
		mPushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);

		K[] oldKeyTable = mKeyTable;
		int[] oldValueTable = mValueTable;

		mKeyTable = (K[])new Object[newSize + mStashCapacity];
		mValueTable = new int[newSize + mStashCapacity];

		int oldSize = size;
		size = 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				K key = oldKeyTable[i];
				if (key != null) putResize(key, oldValueTable[i]);
			}
		}
	}

	private int hash2 (int h) {
		h *= PRIME2;
		return (h ^ h >>> mHashShift) & mMask;
	}

	private int hash3 (int h) {
		h *= PRIME3;
		return (h ^ h >>> mHashShift) & mMask;
	}

    @Override
	public String toString () {
		if (size == 0) return "{}";
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		K[] keyTable = this.mKeyTable;
		int[] valueTable = this.mValueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append('}');
		return buffer.toString();
	}
}
