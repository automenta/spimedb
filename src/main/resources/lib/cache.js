/**
 * A cache that can exhibit both least recently used (LRU) and max time to live (TTL) eviction policies.
 *
 * Internally the cache is backed by a `Map` but also maintains a linked list of entries to support the eviction policies.
 *
 * from: https://www.npmjs.com/package/es6-lru-cache?activeTab=code
 */
class Cache {

    // cache entries are objects with
    //   key - duplicated here to make iterator based methods more efficient
    //   value
    //   prev - a pointer
    //   next - a pointer
    //   expires - time of death in Date.now

    /**
     *
     * @param {number} ttl - the max. time to live, in milliseconds
     * @param {number} max - the max. number of entries in the cache
     * @param {Object|Iterable} data - the data to initialize the cache with
     */
    constructor ({ttl, max, data = {}}) {
        this.data = new Map()
        if (max) { this.max = max }
        if (ttl) { this.ttl = ttl }
        // this.head = undefined
        // this.tail = undefined
        if (data) {
            if (data[Symbol.iterator]) {
                for (let [key, value] in data) {
                    this.set(key, value)
                }
            } else {
                Object.keys(data).forEach(key => this.set(key, data[key]))
            }
        }
    }

    clear () {
        this.data.clear()
        this.head = undefined
        this.tail = undefined
    }

    delete (key) {
        const curr = this.data.get(key)
        if (this.data.delete(key)) {
            this._remove(curr)
            return true
        }
        return false
    }

    entries () {
        return this._iterator(entry => [entry.key, entry.value])
    }

    evict () {
        let count = 0
        let max = this.max
        let now = this.ttl ? Date.now() : false
        for (let curr = this.head; curr; curr = curr.next) {
            ++count
            if ((max && max < count) || (now && now > curr.expires)) {
                this.data.delete(curr.key)
                this._remove(curr)
            }
        }
        return count
    }

    forEach (callback) {
        const iterator = this._iterator(entry => {
            callback(entry.key, entry.value) // todo: support thisArg parameter
            return true
        })
        while (iterator.next()) { /* no-op */ }
    }

    get (key) {
        const entry = this.data.get(key)
        if (entry) {
            if (entry.expires && entry.expires < Date.now()) {
                this.delete(key)
            } else {
                return entry.value
            }
        }
    }

    has (key) {
        const entry = this.data.get(key)
        if (entry) {
            if (entry.expires && entry.expires < Date.now()) {
                this.delete(key)
            } else {
                return true
            }
        }
        return false
    }

    keys () {
        return this._iterator(entry => entry.key)
    }

    set (key, value) {
        let curr = this.data.get(key)
        if (curr) {
            this._remove(curr)
        } else {
            this.data.set(key, curr = {})
        }
        curr.key = key
        curr.value = value
        if (this.ttl) { curr.expires = Date.now() + this.ttl }
        this._insert(curr)
        this.evict()
        return this
    }

    get size () {
        // run an eviction then we will report the correct size
        return this.evict()
    }

    values () {
        return this._iterator(entry => entry.value)
    }

    [Symbol.iterator] () {
        return this._iterator(entry => [entry.key, entry.value])
    }

    /**
     * @param {Function} accessFn - the function used to convert entries into return values
     * @returns {{next: (function())}}
     * @private
     */
    _iterator (accessFn) {
        const max = this.max
        let now = this.ttl ? Date.now() : false
        let curr = this.head
        let count = 0
        return {
            next: () => {
                while (curr && (count > max || now > curr.expires)) { // eslint-disable-line no-unmodified-loop-condition
                    this.data.delete(curr.key)
                    this._remove(curr)
                    curr = curr.next
                }
                const it = curr
                curr = curr && curr.next
                return it ? accessFn(it) : undefined
            }
        }
    }

    /**
     * Remove entry `curr` from the linked list.
     * @private
     */
    _remove (curr) {
        if (!curr.prev) {
            this.head = curr.next
        } else {
            curr.prev.next = curr.next
        }
        if (!curr.next) {
            this.tail = curr.prev
        } else {
            curr.next.prev = curr.prev
        }
    }

    /**
     * Insert entry `curr` into the head of the linked list.
     * @private
     */
    _insert (curr) {
        if (!this.head) {
            this.head = curr
            this.tail = curr
        } else {
            const node = this.head
            curr.prev = node.prev
            curr.next = node
            if (!node.prev) {
                this.head = curr
            } else {
                node.prev.next = curr
            }
            node.prev = curr
        }
    }
}
